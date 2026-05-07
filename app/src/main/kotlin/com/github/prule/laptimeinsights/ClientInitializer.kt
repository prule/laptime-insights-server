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
  private var sessionStartPhases =
    listOf(
      AccBroadcastingInbound.SessionPhase.SESSION,
      AccBroadcastingInbound.SessionPhase.PRE_SESSION,
    )

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

              /* First "session event" means session has started */
              if (session == null && sessionStartPhases.contains(message.phase())) {
                sessionState = SessionState()
                session =
                  appModule.session.createSessionUseCase.createSession(
                    CreateSessionCommand(
                      Simulator.ACC,
                      SessionType(message.sessionType()?.name ?: "Unknown"),
                      track,
                      car,
                    )
                  )

                appModule.session.startSessionUseCase.startSession(
                  StartSessionCommand(session!!.uid, Clock.System.now())
                )
                logger.info("Session started")
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
            bestLapTimeMs = message.bestSessionLap()?.lapTimeMs()?.toLong() ?: Long.MAX_VALUE,
            lastLapTimeMs = message.lastLap()?.lapTimeMs()?.toLong() ?: Long.MAX_VALUE,
            currentLapTimeMs = message.currentLap()?.lapTimeMs()?.toLong() ?: 0L,
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

            logger.info("Lap completed ${JsonFormatter.toJsonString(message as Any)}")
          }
        }
      ),
    )
  }

  private fun buildEntryListCar(): MessageListener<AccBroadcastingInbound> {
    return ConditionalFilter(
      condition = { message ->
        message.msgType() == AccBroadcastingInbound.InboundMsgType.ENTRY_LIST_CAR &&
          context.focusedCarIndex != null
      },
      clazz = AccBroadcastingInbound.EntryListCar::class,
      block = { message, _ ->
        val resolvedCar =
          appModule.car.findCarUseCase.findCarByModel(FindCarCommand(message.carModelType()))
        sessionState?.registerCar(CarId(message.carId()), resolvedCar)
        logger.info("Car registered: carId=${message.carId()} car=$resolvedCar")

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
