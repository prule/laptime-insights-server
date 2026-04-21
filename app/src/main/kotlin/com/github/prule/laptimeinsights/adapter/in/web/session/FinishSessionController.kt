package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.FinishSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.FinishSessionUseCase
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlin.time.Instant
import kotlinx.serialization.Serializable

class FinishSessionController(
  application: Application,
  finishSessionUseCase: FinishSessionUseCase,
) {
  init {
    application.routing {
      post<SessionRoutes.SessionId.Finish> { finish ->
        val request = call.receive<FinishSessionRequest>()
        call.respond(
          SessionResource.fromDomain(
            finishSessionUseCase.finishSession(
              FinishSessionCommand(uid = Uid(finish.parent.uid), finishedAt = request.finishedAt)
            ),
            SessionLinkFactory(application),
          )
        )
      }
    }
  }
}

@Serializable data class FinishSessionRequest(val finishedAt: Instant)
