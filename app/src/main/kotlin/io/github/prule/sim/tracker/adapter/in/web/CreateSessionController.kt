package io.github.prule.sim.tracker.adapter.`in`.web

import io.github.prule.sim.tracker.application.port.`in`.CreateSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.CreateSessionUseCase
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class CreateSessionController(
    application: Application,
    createSessionUseCase: CreateSessionUseCase,
) {
    init {
        application.routing {
            post("/api/1/session") {
                call.respond(
                    SessionResource.fromDomain(
                        application,
                        createSessionUseCase.createSession(
                            call.receive<CreateSessionCommand>(),
                        ),
                    ),
                )
            }
        }
    }
}
