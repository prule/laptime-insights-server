package com.github.prule.laptimeinsights

import com.github.prule.acc.client.AccClient
import com.github.prule.acc.client.AccClientConfiguration
import com.github.prule.acc.client.ClientContext
import com.github.prule.acc.client.ContextUpdater
import com.github.prule.acc.client.FilteredMessageListener
import com.github.prule.acc.client.JsonFormatter
import com.github.prule.acc.client.LoggingListener
import com.github.prule.acc.client.MessageListener
import com.github.prule.acc.client.MessageSender
import com.github.prule.acc.messages.AccBroadcastingInbound
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.`in`.car.FindCarCommand
import com.github.prule.laptimeinsights.application.port.`in`.car.RecordRealtimeCarUpdateCommand
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.EndSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.StartSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.UpdateSessionCommand
import kotlin.reflect.KClass
import kotlin.time.Clock
import org.slf4j.LoggerFactory

class ClientInitializer(private val appModule: AppModule) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var context = ClientContext()
  private var session: Session? = null
  private var sessionState: SessionState? = null
  private var track: Track? = null
  private var car: Car? = null
  private val sessionTracker = SessionTracker()

  suspend fun initializeClient(configuration: ApplicationClientConfiguration) {

    AccClient(
        AccClientConfiguration("Test", port = configuration.port, serverIp = configuration.serverIp)
      )
      .connect(
        listOf(
          LoggingListener(),
          ContextUpdater(context),
          buildRealTimeUpdate(),
          buildRealTimeCarUpdate(),
          buildLapCompleted(),
          buildEntryListCar(),
          buildTrackData(),
        )
      )
  }

  /**
   * Opens a new session for the current [track]/[car]. A fresh [SessionState] is installed so lap
   * counts and per-car validity restart per session and never leak from a previous one.
   */
  private fun startNewSession(sessionType: String) {
    sessionState = SessionState()
    session =
      appModule.session.createSessionUseCase.createSession(
        CreateSessionCommand(Simulator.ACC, SessionType(sessionType), track, car)
      )
    appModule.session.startSessionUseCase.startSession(
      StartSessionCommand(session!!.uid, Clock.System.now())
    )
    logger.info("Session started")
  }

  /** Finalizes the active session (recording its end time) and clears per-session state. */
  private fun endCurrentSession() {
    val current = session ?: return
    appModule.session.endSessionUseCase.endSession(
      EndSessionCommand(current.uid, Clock.System.now())
    )
    logger.info("Session ended")
    session = null
    sessionState = null
  }

  private fun buildRealTimeUpdate():
    FilteredMessageListener<AccBroadcastingInbound.RealtimeUpdate> {
    return FilteredMessageListener(
      clazz = AccBroadcastingInbound.RealtimeUpdate::class,
      // listeners to apply to filtered messages
      listeners =
        listOf(
          object : MessageListener<AccBroadcastingInbound.RealtimeUpdate> {

            override fun onMessage(
              bytes: ByteArray,
              message: AccBroadcastingInbound.RealtimeUpdate,
              messageSender: MessageSender,
            ) {

              // Segment the stream into discrete sessions: identity changes and terminal phases
              // open/close sessions so consecutive races aren't merged. See SessionTracker.
              val sessionType = message.sessionType()?.name ?: "Unknown"
              val boundary =
                sessionTracker.observe(message.sessionIndex(), sessionType, message.phase())
              // Log every boundary decision so captured weekends can confirm the phase sequences
              // practice/qualifying actually present at join time.
              logger.debug(
                "Session boundary: index={} type={} phase={} -> {}",
                message.sessionIndex(),
                sessionType,
                message.phase(),
                boundary,
              )
              when (boundary) {
                is SessionBoundary.Start -> startNewSession(sessionType)
                is SessionBoundary.EndThenStart -> {
                  endCurrentSession()
                  startNewSession(sessionType)
                }
                SessionBoundary.End -> endCurrentSession()
                SessionBoundary.Continue -> {}
              }

              if (session != null && session!!.playerCarId?.value != message.focusedCarIndex()) {

                val carEntry = context.cars[message.focusedCarIndex()]
                val carModel =
                  appModule.car.findCarUseCase.findCarByModel(
                    FindCarCommand(carEntry?.carModelType ?: -1)
                  )

                appModule.session.updateSessionUseCase.update(
                  UpdateSessionCommand(
                    session!!.uid,
                    playerCarId = CarId(message.focusedCarIndex()),
                    car = Car(carModel.value),
                  )
                )
              }
            }
          }
        ),
    )
  }

  private fun buildRealTimeCarUpdate(): MessageListener<AccBroadcastingInbound> {
    return ConditionalFilter(
      condition = { message ->
        session != null &&
          message.msgType() == AccBroadcastingInbound.InboundMsgType.REALTIME_CAR_UPDATE
      },
      clazz = AccBroadcastingInbound.RealtimeCarUpdate::class,
      block = { message, _ ->
        val currentSession = session ?: return@ConditionalFilter
        val isPlayerCar = context.focusedCarIndex == message.carIndex()
        // Keep per-car validity in sync so buildLapCompleted reads the correct flag.
        sessionState?.updateCurrentLapValidity(
          CarId(message.carIndex()),
          message.currentLap()?.isInvalid() != 1,
        )
        appModule.car.recordRealtimeCarUpdateUseCase.record(
          RecordRealtimeCarUpdateCommand(
            sessionId = currentSession.id,
            sessionUid = currentSession.uid,
            lapId = null,
            lapUid = null,
            recordedAt = Clock.System.now(),
            carIndex = CarId(message.carIndex()),
            driverIndex = message.driverIndex(),
            driverCount = message.driverCount(),
            gear = message.gear().toInt(),
            worldPosX = message.worldPosX(),
            worldPosY = message.worldPosY(),
            yaw = message.yaw(),
            carLocation = message.carLocation()?.name ?: "UNKNOWN",
            kmh = message.kmh(),
            racePosition = message.position(),
            cupPosition = message.cupPosition(),
            trackPosition = message.trackPosition(),
            splinePosition = message.splinePosition().toDouble(),
            laps = message.laps(),
            delta = message.delta(),
            // ACC's broadcast SDK returns Int.MAX_VALUE (2_147_483_647 ms ≈ 35791 min) as the
            // "no lap yet" sentinel inside a non-null LapInfo. Normalize that to our own
            // Long.MAX_VALUE sentinel so downstream consumers have a single value to check.
            bestLapTimeMs =
              message.bestSessionLap()?.lapTimeMs()?.normalizeLapMs() ?: Long.MAX_VALUE,
            lastLapTimeMs = message.lastLap()?.lapTimeMs()?.normalizeLapMs() ?: Long.MAX_VALUE,
            // Current lap uses 0 (not the sentinel) when no timed lap is in progress.
            currentLapTimeMs =
              message.currentLap()?.lapTimeMs()?.let {
                if (it == Int.MAX_VALUE) 0L else it.toLong()
              } ?: 0L,
            currentLapIsInvalid = message.currentLap()?.isInvalid() == 1,
            currentLapIsOutlap = message.currentLap()?.isOutlap() == 1,
            currentLapIsInlap = message.currentLap()?.isInlap() == 1,
            isPlayerCar = isPlayerCar,
          )
        )
      },
    )
  }

  private fun buildLapCompleted():
    FilteredMessageListener<AccBroadcastingInbound.BroadcastingEvent> {
    return FilteredMessageListener(
      AccBroadcastingInbound.BroadcastingEvent::class,
      { message ->
        session != null && message.type() == AccBroadcastingInbound.BroadcastType.LAPCOMPLETED
      },
      // listeners to apply to filtered messages
      listOf(
        object : MessageListener<AccBroadcastingInbound.BroadcastingEvent> {

          override fun onMessage(
            bytes: ByteArray,
            message: AccBroadcastingInbound.BroadcastingEvent,
            messageSender: MessageSender,
          ) {

            val carId = CarId(message.carId())
            val lapNumber = sessionState!!.incrementLapCount(carId)
            // PB is derived inside CreateLapService — we just report what telemetry observed.
            val createdLap =
              appModule.lap.createLapUseCase.createLap(
                CreateLapCommand(
                  session!!.uid,
                  Clock.System.now(),
                  carId,
                  sessionState!!.getCarModel(carId),
                  LapTimeMs.fromString(message.msg().data()),
                  lapNumber,
                  ValidLap(sessionState!!.isValidLap(carId, lapNumber)),
                )
              )

            // Back-fill the lap reference onto the telemetry frames captured during this lap; they
            // were recorded with a null lapUid before the Lap row existed.
            appModule.car.linkLapTelemetryUseCase.linkCompletedLap(
              sessionUid = session!!.uid,
              carIndex = carId,
              lapNumber = lapNumber.value,
              lapId = createdLap.id,
              lapUid = createdLap.uid,
            )

            logger.info("Lap completed ${JsonFormatter.toJsonString(message as Any)}")
          }
        }
      ),
    )
  }

  private fun buildEntryListCar(): MessageListener<AccBroadcastingInbound> {
    return ConditionalFilter(
      condition = { message ->
        message.msgType() == AccBroadcastingInbound.InboundMsgType.ENTRY_LIST_CAR
      },
      clazz = AccBroadcastingInbound.EntryListCar::class,
      block = { message, _ ->
        val carId = CarId(message.carId())
        val resolvedCar =
          appModule.car.findCarUseCase.findCarByModel(FindCarCommand(message.carModelType()))
        // Register the model whenever it arrives — never gated on the focused car being known —
        // so registrations that precede the focused-car identification are not dropped.
        sessionState?.registerCar(carId, resolvedCar)
        logger.info("Car registered: carId=${message.carId()} car=$resolvedCar")

        // Back-fill any laps that completed for this car before its model was known.
        session?.let {
          val filled = appModule.lap.recordCarOnLapsUseCase.fillMissingCar(it.uid, carId, resolvedCar)
          if (filled > 0) logger.info("Back-filled car on $filled lap(s): carId=${message.carId()}")
        }

        if (message.carId() == context.focusedCarIndex) {
          car = resolvedCar
          if (session != null) {
            appModule.session.updateSessionUseCase.update(
              UpdateSessionCommand(
                uid = session!!.uid,
                track = track,
                car = car,
                playerCarId = context.focusedCarIndex?.let { CarId(it) },
              )
            )
          }
        }
      },
    )
  }

  private fun buildTrackData(): MessageListener<AccBroadcastingInbound> {
    return ConditionalFilter(
      condition = { message ->
        message.msgType() == AccBroadcastingInbound.InboundMsgType.TRACK_DATA
      },
      clazz = AccBroadcastingInbound.TrackData::class,
      block = { message, _ ->
        track = Track(message.trackName().data())
        if (session != null) {
          appModule.session.updateSessionUseCase.update(
            UpdateSessionCommand(session!!.uid, track, car)
          )
        }
      },
    )
  }
}

/**
 * Map ACC's in-band "no lap" sentinel ([Int.MAX_VALUE]) to `null` so callers can chain `?:
 * Long.MAX_VALUE` to convert it to our own sentinel. Any other value is widened to Long.
 */
private fun Int.normalizeLapMs(): Long? = if (this == Int.MAX_VALUE) null else this.toLong()

@Suppress("UNCHECKED_CAST")
class ConditionalFilter<T : AccBroadcastingInbound, SUB : Any>(
  private val condition: (message: T) -> Boolean,
  private val clazz: KClass<SUB>,
  private val block: (message: SUB, messageSender: MessageSender) -> Unit,
) : MessageListener<T> {
  override fun onMessage(bytes: ByteArray, message: T, messageSender: MessageSender) {
    if (condition(message)) {
      if (clazz.isInstance(message.body())) {
        block(message.body() as SUB, messageSender)
      }
    }
  }
}
