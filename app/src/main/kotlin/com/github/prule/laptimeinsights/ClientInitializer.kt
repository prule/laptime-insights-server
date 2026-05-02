package com.github.prule.laptimeinsights

import com.github.prule.acc.client.AccClient
import com.github.prule.acc.client.AccClientConfiguration
import com.github.prule.acc.client.ClientState
import com.github.prule.acc.client.FilteredMessageListener
import com.github.prule.acc.client.JsonFormatter
import com.github.prule.acc.client.LoggingListener
import com.github.prule.acc.client.MessageListener
import com.github.prule.acc.client.MessageSender
import com.github.prule.acc.client.RegistrationResultListener
import com.github.prule.acc.messages.AccBroadcastingInbound
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.`in`.car.FindCarCommand
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.FinishSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.StartSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.UpdateSessionCommand
import kotlin.reflect.KClass
import kotlin.time.Clock
import org.slf4j.LoggerFactory

class ClientInitializer(private val appModule: AppModule) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var clientState: ClientState? = null
  private var session: Session? = null
  private var sessionState: SessionState? = null
  private var track: Track? = null
  private var car: Car? = null
  private var sessionStartPhases =
    listOf(
      AccBroadcastingInbound.SessionPhase.SESSION,
      AccBroadcastingInbound.SessionPhase.PRE_SESSION,
    )
  private var sessionFinishPhases =
    listOf(
      AccBroadcastingInbound.SessionPhase.SESSION_OVER,
      AccBroadcastingInbound.SessionPhase.POST_SESSION,
    )

  suspend fun initializeClient(configuration: ApplicationClientConfiguration) {
    clientState = ClientState()

    AccClient(
        AccClientConfiguration("Test", port = configuration.port, serverIp = configuration.serverIp)
      )
      .connect(
        listOf(
          LoggingListener(),
          RegistrationResultListener(clientState!!),
          buildRealTimeUpdate(),
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

              if (session != null && sessionFinishPhases.contains(message.phase())) {
                appModule.session.finishSessionUseCase.finishSession(
                  FinishSessionCommand(session!!.uid, Clock.System.now())
                )
                session = null
                sessionState = null
                logger.info("Session ended")
              }
            }
          }
        ),
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
            appModule.lap.createLapUseCase.createLap(
              CreateLapCommand(
                session!!.uid,
                Clock.System.now(),
                carId,
                LapTimeMs.fromString(message.msg().data()),
                lapNumber,
                ValidLap(sessionState!!.isValidLap(carId, lapNumber)),
                PersonalBest(false), // TODO
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
          clientState?.focusedCarIndex != null
      },
      clazz = AccBroadcastingInbound.EntryListCar::class,
      block = { message, _ ->
        if (message.carId() == clientState?.focusedCarIndex) {
          car = appModule.car.findCarUseCase.findCarByModel(FindCarCommand(message.carModelType()))
          logger.info("Car is $car")
          if (session != null) {
            appModule.session.updateSessionUseCase.update(
              UpdateSessionCommand(session!!.uid, track, car)
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
