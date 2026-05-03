package com.github.prule.laptimeinsights.adapter.out.persistence.seed

import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapTable
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionTable
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapPort
import com.github.prule.laptimeinsights.application.port.out.session.CreateSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Populates the database with sample sessions and laps.
 *
 * Only intended for local development and demos. The seeder is idempotent: it skips seeding when
 * the tables already contain data.
 *
 * Activated via the `DB_SEED` environment variable (see
 * [com.github.prule.laptimeinsights.EnvironmentVariables.shouldSeedDatabase]).
 */
class DatabaseSeeder(
  private val createSessionPort: CreateSessionPort,
  private val updateSessionPort: UpdateSessionPort,
  private val createLapPort: CreateLapPort,
  private val clock: () -> Instant = { Clock.System.now() },
  private val random: Random = Random(42),
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun seed() {
    transaction {
      val sessionCount = SessionTable.selectAll().count()
      val lapCount = LapTable.selectAll().count()
      if (sessionCount > 0 || lapCount > 0) {
        logger.info(
          "Skipping seeding: data already present (sessions=$sessionCount laps=$lapCount)"
        )
        return@transaction
      }

      logger.info("Seeding database with ${SAMPLE_PROFILES.size} sample sessions")
      val now = clock()
      var totalLaps = 0

      SAMPLE_PROFILES.forEachIndexed { index, profile ->
        val daysAgo = (index + 1).toLong()
        val startedAt = now - daysAgo.days
        val saved = createSessionPort.create(buildSession(profile))
        saved.start(startedAt)
        updateSessionPort.update(saved)

        val laps = generateLaps(profile, saved, startedAt)
        laps.forEach { createLapPort.create(it) }
        totalLaps += laps.size

        saved.finish(startedAt + profile.baseLapTime.times(laps.size))
        updateSessionPort.update(saved)
      }

      logger.info("Seeded ${SAMPLE_PROFILES.size} sessions and $totalLaps laps")
    }
  }

  private fun buildSession(profile: SeedProfile): Session =
    Session(
      id = SessionId(0),
      uid = Uid(),
      startedAt = null,
      finishedAt = null,
      simulator = profile.simulator,
      track = profile.track,
      car = profile.car,
      sessionType = profile.sessionType,
    )

  private fun generateLaps(
    profile: SeedProfile,
    session: Session,
    sessionStart: Instant,
  ): List<Lap> {
    val laps = mutableListOf<Lap>()
    var bestSoFar = Long.MAX_VALUE
    var recordedAt = sessionStart
    for (lapIndex in 1..profile.lapCount) {
      // First lap is treated as an out lap; subsequent laps wobble within a tighter window.
      val variation =
        if (lapIndex == 1) profile.baseLapTime.inWholeMilliseconds / 12
        else random.nextLong(-1500, 2000)
      val lapTimeMs = profile.baseLapTime.inWholeMilliseconds + variation
      val valid = random.nextInt(0, 20) != 0 // ~5% invalid laps
      val isPersonalBest = valid && lapTimeMs < bestSoFar
      if (isPersonalBest) bestSoFar = lapTimeMs

      recordedAt += lapTimeMs.milliseconds
      laps +=
        Lap(
          id = LapId(0),
          uid = Uid(),
          sessionId = session.id,
          sessionUId = session.uid,
          carId = profile.carId,
          recordedAt = recordedAt,
          lapTime = LapTimeMs(lapTimeMs),
          lapNumber = LapNumber(lapIndex),
          valid = ValidLap(valid),
          personalBest = PersonalBest(isPersonalBest),
        )
    }
    return laps
  }

  private data class SeedProfile(
    val simulator: Simulator,
    val track: Track,
    val car: Car,
    val carId: CarId,
    val sessionType: SessionType,
    val lapCount: Int,
    val baseLapTime: Duration,
  )

  companion object {
    private val SAMPLE_PROFILES =
      listOf(
        SeedProfile(
          Simulator.ACC,
          Track("Monza"),
          Car("Ferrari 488 GT3"),
          CarId(20),
          SessionType("Practice"),
          12,
          1.minutes + 47000.milliseconds,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Spa-Francorchamps"),
          Car("Porsche 991 II GT3 R"),
          CarId(23),
          SessionType("Qualifying"),
          8,
          2.minutes + 16500.milliseconds,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Nurburgring"),
          Car("Mercedes-AMG GT3"),
          CarId(1),
          SessionType("Race"),
          18,
          1.minutes + 54200.milliseconds,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Silverstone"),
          Car("Audi R8 LMS Evo"),
          CarId(31),
          SessionType("Practice"),
          14,
          1.minutes + 58800.milliseconds,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Snetterton"),
          Car("McLaren 720S GT3"),
          CarId(30),
          SessionType("Race"),
          20,
          1.minutes + 45100.milliseconds,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Brands Hatch"),
          Car("BMW M4 GT3"),
          CarId(30),
          SessionType("Qualifying"),
          10,
          1.minutes + 23200.milliseconds,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Monza"),
          Car("Porsche 991 II GT3 R"),
          CarId(23),
          SessionType("Race"),
          22,
          1.minutes + 48400.milliseconds,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Spa-Francorchamps"),
          Car("Ferrari 488 GT3"),
          CarId(20),
          SessionType("Practice"),
          16,
          2.minutes + 17800.milliseconds,
        ),
      )
  }
}
