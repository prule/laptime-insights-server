package com.github.prule.laptimeinsights.adapter.out.persistence.car

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.RealtimeCarUpdate
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlin.time.Clock
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class RealtimeCarUpdateRepositoryTest : RepositoryTest(listOf(RealtimeCarUpdateTable)) {

  private val repository = RealtimeCarUpdateRepository()

  private val sessionUid = Uid("session-1")

  /** A telemetry frame for [car] captured while it had completed [completedLaps] laps. */
  private fun frame(car: Int, completedLaps: Int, spline: Double) =
    RealtimeCarUpdate(
      sessionId = SessionId(1),
      sessionUid = sessionUid,
      lapId = null,
      lapUid = null,
      recordedAt = Clock.System.now(),
      carIndex = CarId(car),
      driverIndex = 0,
      driverCount = 1,
      gear = 3,
      worldPosX = 0f,
      worldPosY = 0f,
      yaw = 0f,
      carLocation = "TRACK",
      kmh = 200,
      racePosition = 1,
      cupPosition = 1,
      trackPosition = 1,
      splinePosition = spline,
      laps = completedLaps,
      delta = 0,
      bestLapTimeMs = Long.MAX_VALUE,
      lastLapTimeMs = Long.MAX_VALUE,
      currentLapTimeMs = 0,
      currentLapIsInvalid = false,
      currentLapIsOutlap = false,
      currentLapIsInlap = false,
    )

  @Test
  fun `links a completed lap's in-progress frames and leaves other cars and later laps untouched`() {
    transaction {
      // Car 10: 3 frames during lap 1 (laps=0), 2 frames during lap 2 (laps=1).
      repository.create(frame(car = 10, completedLaps = 0, spline = 0.1))
      repository.create(frame(car = 10, completedLaps = 0, spline = 0.5))
      repository.create(frame(car = 10, completedLaps = 0, spline = 0.9))
      repository.create(frame(car = 10, completedLaps = 1, spline = 0.2))
      repository.create(frame(car = 10, completedLaps = 1, spline = 0.6))
      // Car 20: 1 frame during its lap 1 — must stay untouched.
      repository.create(frame(car = 20, completedLaps = 0, spline = 0.3))

      // Lap 1 for car 10 completes → link its laps<1 frames.
      val linked =
        repository.linkFramesToLap(
          sessionUid = sessionUid,
          carIndex = CarId(10),
          lapNumber = 1,
          lapId = LapId(100),
          lapUid = Uid("lap-1-uid"),
        )

      assertThat(linked).isEqualTo(3)
      assertThat(repository.findByLapUid(Uid("lap-1-uid"))).hasSize(3)
      // Car 20's frame and car 10's lap-2 frames are still unlinked.
      assertThat(repository.findByLapUid(Uid("lap-2-uid"))).isEmpty()

      // Lap 2 completes → links only the remaining laps<2 unlinked frames (the laps=1 ones).
      val linked2 =
        repository.linkFramesToLap(
          sessionUid = sessionUid,
          carIndex = CarId(10),
          lapNumber = 2,
          lapId = LapId(101),
          lapUid = Uid("lap-2-uid"),
        )

      assertThat(linked2).isEqualTo(2)
      assertThat(repository.findByLapUid(Uid("lap-2-uid"))).hasSize(2)
      // Lap 1's frames are unchanged.
      assertThat(repository.findByLapUid(Uid("lap-1-uid"))).hasSize(3)
    }
  }
}
