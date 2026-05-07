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
import com.github.prule.laptimeinsights.application.domain.model.RealtimeCarUpdate
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.out.car.CreateRealtimeCarUpdatePort
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapPort
import com.github.prule.laptimeinsights.application.port.out.session.CreateSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
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
 * Populates the database with sample sessions, laps and realtime car updates.
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
  private val createRealtimeCarUpdatePort: CreateRealtimeCarUpdatePort,
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

      SAMPLE_PROFILES.forEach { profile ->
        val startedAt = now - profile.daysAgo.days
        val saved = createSessionPort.create(buildSession(profile))
        saved.start(startedAt)
        updateSessionPort.update(saved)

        val laps = generateLaps(profile, saved, startedAt)
        laps.forEach { lap ->
          val savedLap = createLapPort.create(lap)
          // Player-car laps contribute to the session's drivingTime aggregate.
          // CreateLapService does this in production; the seeder writes laps directly via the
          // port (skipping the service to control PB / event emission), so we mirror the
          // bookkeeping here.
          saved.addDriving(savedLap.lapTime)
          createRealtimeCarUpdatePort.batchCreate(
            generateRealtimeCarUpdates(profile, savedLap, saved)
          )
        }
        totalLaps += laps.size

        // For Race and Qualifying sessions add 2 competitor cars at slightly
        // different pace so the session detail screen has cross-car laps to show.
        if (profile.sessionType.value in listOf("Race", "Qualifying")) {
          COMPETITOR_OFFSETS.forEachIndexed { idx, paceOffsetMs ->
            val competitorCarId = CarId(profile.carId.value + idx + 1)
            val competitorLaps =
              generateLaps(
                profile.copy(
                  carId = competitorCarId,
                  baseLapTime = profile.baseLapTime + paceOffsetMs.milliseconds,
                ),
                saved,
                startedAt,
              )
            competitorLaps.forEach { lap ->
              val savedLap = createLapPort.create(lap)
              // Competitor lap → no addDriving; drivingTime tracks player only.
              createRealtimeCarUpdatePort.batchCreate(
                generateRealtimeCarUpdates(profile, savedLap, saved)
              )
            }
            totalLaps += competitorLaps.size
          }
        }

        // Persist the accumulated drivingTime in one update at the end.
        updateSessionPort.update(saved)
      }

      logger.info("Seeded ${SAMPLE_PROFILES.size} sessions and $totalLaps laps")
    }
  }

  /**
   * Synthesizes a smooth, lap-specific set of [RealtimeCarUpdate] frames.
   *
   * Speed follows a sum of sinusoids so each track gets its own corner pattern. Faster laps sit
   * uniformly slightly higher on the speed envelope. Gear is derived from speed buckets.
   *
   * World position is synthesised as a parametric track shape: a base ellipse perturbed by the
   * track's corner count so every track has a recognisably different outline. The shape is
   * deterministic and consistent across laps on the same track.
   */
  private fun generateRealtimeCarUpdates(
    profile: SeedProfile,
    lap: Lap,
    session: Session,
  ): List<RealtimeCarUpdate> {
    val samples = SAMPLES_PER_LAP
    val trackSeed = profile.baseLapTime.inWholeMilliseconds
    val cornerCount = TRACK_CORNERS[profile.track] ?: 8
    val paceFactor =
      1.0 + (profile.baseLapTime.inWholeMilliseconds - lap.lapTime.value).toDouble() / 50_000.0
    val lapJitter = (lap.lapNumber.value * 17 % 7) * 0.4

    val speeds = DoubleArray(samples)
    for (i in 0 until samples) {
      val t = i.toDouble() / samples
      val cornerWave = sin(2.0 * PI * cornerCount * t + (trackSeed % 31) * 0.1)
      val secondaryWave = cos(4.0 * PI * t + lapJitter)
      val baseline = 180.0 + 60.0 * cornerWave + 25.0 * secondaryWave
      speeds[i] = max(60.0, baseline * paceFactor)
    }

    // Synthetic track shape: ellipse + harmonic perturbations keyed to corner count.
    // Radius ~500 m so the scale is vaguely realistic.
    val radiusX = 500.0
    val radiusY = 300.0
    val perturbAmp = 80.0
    val perturbPhase = (trackSeed % 31) * 0.2

    return List(samples) { i ->
      val splinePosition = i.toDouble() / samples
      val speed = speeds[i]
      val gear =
        when {
          speed < 90 -> 2
          speed < 130 -> 3
          speed < 170 -> 4
          speed < 210 -> 5
          else -> 6
        }
      val angle = 2.0 * PI * splinePosition
      val perturb = perturbAmp * sin(cornerCount * angle + perturbPhase)
      val wx = ((radiusX + perturb) * cos(angle)).toFloat()
      val wy = ((radiusY + perturb * 0.5) * sin(angle)).toFloat()
      RealtimeCarUpdate(
        sessionId = session.id,
        sessionUid = session.uid,
        lapId = lap.id,
        lapUid = lap.uid,
        recordedAt = lap.recordedAt,
        carIndex = lap.carId,
        driverIndex = 0,
        driverCount = 1,
        gear = gear,
        worldPosX = wx,
        worldPosY = wy,
        yaw = angle.toFloat(),
        carLocation = "TRACK",
        kmh = speed.toInt(),
        racePosition = 1,
        cupPosition = 1,
        trackPosition = 0,
        splinePosition = splinePosition,
        laps = lap.lapNumber.value - 1,
        delta = 0,
        bestLapTimeMs = Long.MAX_VALUE,
        lastLapTimeMs = Long.MAX_VALUE,
        currentLapTimeMs = lap.lapTime.value,
        currentLapIsInvalid = !lap.valid.value,
        currentLapIsOutlap = lap.lapNumber.value == 1,
        currentLapIsInlap = false,
      )
    }
  }

  private fun buildSession(profile: SeedProfile): Session =
    Session(
      id = SessionId(0),
      uid = Uid(),
      startedAt = null,
      simulator = profile.simulator,
      track = profile.track,
      car = profile.car,
      sessionType = profile.sessionType,
      playerCarId = profile.carId,
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
      val variation =
        if (lapIndex == 1) profile.baseLapTime.inWholeMilliseconds / 12
        else random.nextLong(-1500, 2000)
      val lapTimeMs = profile.baseLapTime.inWholeMilliseconds + variation
      val valid = random.nextInt(0, 20) != 0
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
          car = profile.car,
          recordedAt = recordedAt,
          lapTime = LapTimeMs(lapTimeMs),
          lapNumber = LapNumber(lapIndex),
          valid = ValidLap(valid),
          personalBest = PersonalBest(isPersonalBest),
          track = profile.track,
          playerLap = profile.carId == session.playerCarId,
        )
    }
    return laps
  }

  /**
   * @param daysAgo session start, expressed as days back from `clock()`. Values are deliberately
   *   spread across every dashboard time-range bucket (1m / 3m / 6m / 1y / all) so the selector
   *   visibly changes the dataset.
   */
  private data class SeedProfile(
    val simulator: Simulator,
    val track: Track,
    val car: Car,
    val carId: CarId,
    val sessionType: SessionType,
    val lapCount: Int,
    val baseLapTime: Duration,
    val daysAgo: Long,
  )

  companion object {
    private const val SAMPLES_PER_LAP = 150

    private val COMPETITOR_OFFSETS = listOf(+1_800L, -900L)

    private val TRACK_CORNERS: Map<Track, Int> =
      mapOf(
        Track("Monza") to 11,
        Track("Spa-Francorchamps") to 19,
        Track("Nurburgring") to 16,
        Track("Silverstone") to 18,
        Track("Snetterton") to 12,
        Track("Brands Hatch") to 9,
        Track("Monaco") to 19,
        Track("Suzuka") to 18,
      )

    private val SAMPLE_PROFILES =
      listOf(
        // ── 1 month bucket ─────────────────────────────────────────────
        SeedProfile(
          Simulator.ACC,
          Track("Monza"),
          Car("Ferrari 488 GT3"),
          CarId(20),
          SessionType("Practice"),
          12,
          1.minutes + 47000.milliseconds,
          daysAgo = 2,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Spa-Francorchamps"),
          Car("Porsche 991 II GT3 R"),
          CarId(23),
          SessionType("Qualifying"),
          8,
          2.minutes + 16500.milliseconds,
          daysAgo = 8,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Brands Hatch"),
          Car("BMW M4 GT3"),
          CarId(30),
          SessionType("Qualifying"),
          10,
          1.minutes + 23200.milliseconds,
          daysAgo = 16,
        ),
        SeedProfile(
          Simulator.F1,
          Track("Monaco"),
          Car("F1 2026"),
          CarId(0),
          SessionType("Practice"),
          15,
          1.minutes + 12500.milliseconds,
          daysAgo = 25,
        ),
        // ── 3 month bucket ─────────────────────────────────────────────
        SeedProfile(
          Simulator.ACC,
          Track("Nurburgring"),
          Car("Mercedes-AMG GT3"),
          CarId(1),
          SessionType("Race"),
          18,
          1.minutes + 54200.milliseconds,
          daysAgo = 42,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Silverstone"),
          Car("Audi R8 LMS Evo"),
          CarId(31),
          SessionType("Practice"),
          14,
          1.minutes + 58800.milliseconds,
          daysAgo = 60,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Snetterton"),
          Car("McLaren 720S GT3"),
          CarId(30),
          SessionType("Race"),
          20,
          1.minutes + 45100.milliseconds,
          daysAgo = 80,
        ),
        // ── 6 month bucket ─────────────────────────────────────────────
        SeedProfile(
          Simulator.F1,
          Track("Suzuka"),
          Car("F1 2026"),
          CarId(0),
          SessionType("Race"),
          25,
          1.minutes + 31900.milliseconds,
          daysAgo = 120,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Monza"),
          Car("Porsche 991 II GT3 R"),
          CarId(23),
          SessionType("Race"),
          22,
          1.minutes + 48400.milliseconds,
          daysAgo = 165,
        ),
        // ── 1 year bucket ──────────────────────────────────────────────
        SeedProfile(
          Simulator.ACC,
          Track("Spa-Francorchamps"),
          Car("Ferrari 488 GT3"),
          CarId(20),
          SessionType("Practice"),
          16,
          2.minutes + 17800.milliseconds,
          daysAgo = 220,
        ),
        SeedProfile(
          Simulator.ACC,
          Track("Silverstone"),
          Car("BMW M4 GT3"),
          CarId(30),
          SessionType("Race"),
          18,
          1.minutes + 59500.milliseconds,
          daysAgo = 310,
        ),
        // ── all-time bucket (>1 year) ──────────────────────────────────
        SeedProfile(
          Simulator.ACC,
          Track("Nurburgring"),
          Car("Audi R8 LMS Evo"),
          CarId(31),
          SessionType("Qualifying"),
          12,
          1.minutes + 55600.milliseconds,
          daysAgo = 450,
        ),
        SeedProfile(
          Simulator.F1,
          Track("Monaco"),
          Car("F1 2026"),
          CarId(0),
          SessionType("Race"),
          24,
          1.minutes + 13800.milliseconds,
          daysAgo = 640,
        ),
      )
  }
}
