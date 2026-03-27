package io.github.prule.sim.tracker.adapter.`in`.web.session

import io.github.prule.sim.tracker.application.domain.model.Uid
import io.github.prule.sim.tracker.application.port.`in`.session.FinishSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.session.FinishSessionUseCase
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlin.time.Instant

class FinishSessionController(application: Application, finishSessionUseCase: FinishSessionUseCase) {
  init {
    application.routing {
      post<SessionRoutes.SessionId.Finish> { finish ->
        val request = call.receive<FinishSessionRequest>()
        call.respond(
            SessionResource.fromDomain(
                finishSessionUseCase.finishSession(
                  FinishSessionCommand(
                    uid = Uid(finish.parent.uid),
                    finishedAt = request.finishedAt,
                  ),
                ),
                SessionLinkFactory(application),
            ),
        )
      }
    }
  }
}

@Serializable data class FinishSessionRequest(val finishedAt: Instant)
