package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapCommand
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapUseCase
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
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
