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

class ClientInitializer(private val appModule: AppModule) {
  suspend fun initializeClient(configuration: ApplicationClientConfiguration) {
    val clientState = ClientState()
    AccClient(
            AccClientConfiguration(
                "Test",
                port = configuration.port,
                serverIp = configuration.serverIp,
            ),
        )
        .connect(
            listOf(
                LoggingListener(),
                RegistrationResultListener(clientState),
                buildFilter(),
                buildFilter2(),
            ),
        )
  }

  private fun buildFilter(): FilteredMessageListener<AccBroadcastingInbound.RealtimeUpdate> {
    return FilteredMessageListener(
        AccBroadcastingInbound.RealtimeUpdate::class,
        { message -> message.phase() == AccBroadcastingInbound.SessionPhase.SESSION },
        // listeners to apply to filtered messages
        listOf(
            object : MessageListener<AccBroadcastingInbound.RealtimeUpdate> {
              var sessionStarted = false

              override fun onMessage(
                  bytes: ByteArray,
                  message: AccBroadcastingInbound.RealtimeUpdate,
                  messageSender: MessageSender,
              ) {
                /* First "session event" means session has started */
                if (
                    !sessionStarted &&
                        message.phase() == AccBroadcastingInbound.SessionPhase.SESSION
                ) {
                  sessionStarted = true
//                  appModule.session.createSessionUseCase.createSession(
//                      CreateSessionCommand(Simulator.ACC, Track(), message.sessionType())
//                  )
                  println("Session started")
                }

                if (
                    !sessionStarted &&
                        message.phase() == AccBroadcastingInbound.SessionPhase.SESSION_OVER
                ) {
                  println("Session ended")
                }

                println("Session ${JsonFormatter.toJsonString(message as Any)}")
              }
            },
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
                println("Lap completed ${JsonFormatter.toJsonString(message as Any)}")
              }
            },
        ),
    )
  }
}
