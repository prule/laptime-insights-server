package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapCreated
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapCommand
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapUseCase
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapPort
import com.github.prule.laptimeinsights.application.port.out.lap.SearchLapPort
import com.github.prule.laptimeinsights.application.port.out.lap.UpdateLapPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Records a completed lap and derives its `personalBest` flag against the
 * other valid laps already stored for the same session + car.
 *
 * Callers only report what telemetry observed (lap time, validity); PB is a
 * server-side concern so every consumer sees a consistent view without having
 * to repeat the calculation.
 *
 * Algorithm:
 * 1. Persist the new lap with `personalBest = false`.
 * 2. If the lap is valid, look up the current PB for the same session + car.
 *    - If none exists, or the new lap is faster, mark the new lap as PB and
 *      demote the previous PB (if any) to `personalBest = false`.
 * 3. Emit `LapCreated` carrying the lap in its final state.
 *
 * Invalid laps never become PB; their `personalBest` stays false.
 */
class CreateLapService(
  private val createLapPort: CreateLapPort,
  private val searchLapPort: SearchLapPort,
  private val updateLapPort: UpdateLapPort,
  private val searchSessionPort: SearchSessionPort,
  private val eventPort: EventPort,
) : CreateLapUseCase {
  override fun createLap(command: CreateLapCommand): Lap {
    val savedLap = transaction {
      val session =
        searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.sessionUid))
          ?: throw NotFoundException()
      val lap =
        Lap(
          id = LapId(0),
          uid = Uid(),
          sessionId = session.id,
          sessionUId = session.uid,
          carId = command.carId,
          car = command.car,
          // Always persist as non-PB initially — derivation below promotes it
          // (and demotes the previous PB) atomically within this transaction.
          personalBest = PersonalBest(false),
          valid = command.valid,
          recordedAt = command.recordedAt,
          lapTime = command.lapTime,
          lapNumber = command.lapNumber,
        )
      val created = createLapPort.create(lap)

      if (created.valid == ValidLap(true)) {
        val currentBest =
          searchLapPort.searchForOne(
            LapSearchCriteria(
              sessionUid = created.sessionUId,
              carId = created.carId,
              personalBest = PersonalBest(true),
              validLap = ValidLap(true),
            )
          )
        val isNewBest = currentBest == null || created.lapTime.value < currentBest.lapTime.value
        if (isNewBest) {
          if (currentBest != null) {
            updateLapPort.update(currentBest.copy(personalBest = PersonalBest(false)))
          }
          updateLapPort.update(created.copy(personalBest = PersonalBest(true)))
        } else {
          created
        }
      } else {
        created
      }
    }
    eventPort.emit(LapCreated(savedLap))
    return savedLap
  }
}
