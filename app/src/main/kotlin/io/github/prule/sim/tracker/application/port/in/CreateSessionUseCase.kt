package io.github.prule.sim.tracker.application.port.`in`

interface CreateSessionUseCase {
    fun createSession(command: CreateSessionCommand)
}
