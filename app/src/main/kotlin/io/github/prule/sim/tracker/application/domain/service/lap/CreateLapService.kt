package io.github.prule.sim.tracker.application.domain.service.lap

import io.github.prule.sim.tracker.application.domain.model.Lap
import io.github.prule.sim.tracker.application.domain.model.LapId
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.domain.model.Uid
import io.github.prule.sim.tracker.application.port.`in`.lap.CreateLapCommand
import io.github.prule.sim.tracker.application.port.`in`.lap.CreateLapUseCase
import io.github.prule.sim.tracker.application.port.out.lap.CreateLapPort
import io.github.prule.sim.tracker.application.port.out.session.SearchSessionPort
import io.github.prule.sim.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CreateLapService(
    private val createLapPort: CreateLapPort,
    private val searchSessionPort: SearchSessionPort,
) : CreateLapUseCase {
  override fun createLap(command: CreateLapCommand): Lap = transaction {
    val session =
        searchSessionPort.searchForOne(
            SessionSearchCriteria(uid = command.sessionUid),
        ) ?: throw NotFoundException()
    val lap =
        Lap(
            id = LapId(0),
            uid = Uid(),
            sessionId = session.id,
            sessionUId = session.uid,
            personalBest = command.personalBest,
            valid = command.valid,
            recordedAt = command.recordedAt,
            lapTime = command.lapTime,
            lapNumber = command.lapNumber,
        )
    createLapPort.create(lap)
  }
}
