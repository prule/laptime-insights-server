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
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.`in`.car.FindCarCommand
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.FinishSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.UpdateSessionCommand
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.time.Clock

class ClientInitializer(private val appModule: AppModule) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var session: Session? = null
  private var sessionState: SessionState? = null

  suspend fun initializeClient(configuration: ApplicationClientConfiguration) {
    val clientState = ClientState()

    AccClient(
            AccClientConfiguration(
                "Test",
                port = configuration.port,
                serverIp = configuration.serverIp,
            )
        )
        .connect(listOf(LoggingListener(), RegistrationResultListener(clientState), buildFilter(), buildFilter2(), buildFilter3()))
  }

  private fun buildFilter(): FilteredMessageListener<AccBroadcastingInbound.RealtimeUpdate> {
    return FilteredMessageListener(
        AccBroadcastingInbound.RealtimeUpdate::class,
        { message -> message.phase() == AccBroadcastingInbound.SessionPhase.SESSION },
        // listeners to apply to filtered messages
        listOf(
            object : MessageListener<AccBroadcastingInbound.RealtimeUpdate> {

              override fun onMessage(
                  bytes: ByteArray,
                  message: AccBroadcastingInbound.RealtimeUpdate,
                  messageSender: MessageSender,
              ) {

                /* First "session event" means session has started */
                if (session == null && message.phase() == AccBroadcastingInbound.SessionPhase.SESSION) {
                  sessionState = SessionState()
                  session =
                      appModule.session.createSessionUseCase.createSession(
                          CreateSessionCommand(
                              Simulator.ACC,
                              SessionType(message.sessionType()?.name ?: "Unknown"),
                          )
                      )

                  logger.info("Session started")
                }

                if (session != null && message.phase() == AccBroadcastingInbound.SessionPhase.SESSION_OVER) {
                  appModule.session.finishSessionUseCase.finishSession(FinishSessionCommand(session!!.uid, Clock.System.now()))
                  session = null
                  sessionState = null
                  logger.info("Session ended")
                }

                logger.info("Session ${JsonFormatter.toJsonString(message as Any)}")
              }
            }
        ),
    )
  }

  private fun buildFilter2(): FilteredMessageListener<AccBroadcastingInbound.BroadcastingEvent> {
    return FilteredMessageListener(
        AccBroadcastingInbound.BroadcastingEvent::class,
        { message -> message.type() == AccBroadcastingInbound.BroadcastType.LAPCOMPLETED },
        // listeners to apply to filtered messages
        listOf(
            object : MessageListener<AccBroadcastingInbound.BroadcastingEvent> {

              override fun onMessage(
                  bytes: ByteArray,
                  message: AccBroadcastingInbound.BroadcastingEvent,
                  messageSender: MessageSender,
              ) {

                val lapNumber = sessionState!!.incrementLapCount(message.carId())
                appModule.lap.createLapUseCase.createLap(
                    CreateLapCommand(
                        session!!.uid,
                        Clock.System.now(),
                        LapTimeMs.fromString(message.msg().data()),
                        lapNumber,
                        ValidLap(sessionState!!.isValidLap(message.carId(), lapNumber)),
                        PersonalBest(false), // TODO
                    )
                )

                logger.info("Lap completed ${JsonFormatter.toJsonString(message as Any)}")
              }
            }
        ),
    )
  }

  private fun buildFilter3(): MessageListener<AccBroadcastingInbound> {
    return ConditionalFilter(
        condition = { message -> session != null },
        clazz = AccBroadcastingInbound.EntryListCar::class,
        block = { message, _ ->
          appModule.session.updateSessionUseCase.update(
              UpdateSessionCommand(session!!.uid, null, appModule.car.findCarUseCase.findCarByModel(FindCarCommand(message.carModelType())))
          )
        },
    )
  }
}

@Suppress("UNCHECKED_CAST")
class ConditionalFilter<T, SUB : Any>(
    private val condition: (message: T) -> Boolean,
    private val clazz: KClass<SUB>,
    private val block:
        (
            message: SUB,
            messageSender: MessageSender,
        ) -> Unit,
) : MessageListener<T> {
  override fun onMessage(bytes: ByteArray, message: T, messageSender: MessageSender) {
    if (condition(message)) {
      if (clazz.isInstance(message)) {
        block(message as SUB, messageSender)
      }
    }
  }
}
