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
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapPort
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapTelemetryPort
import com.github.prule.laptimeinsights.application.port.out.session.CreateSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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
  private val createLapTelemetryPort: CreateLapTelemetryPort,
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
        laps.forEach { lap ->
          val savedLap = createLapPort.create(lap)
          createLapTelemetryPort.create(
            lapId = savedLap.id,
            lapUid = savedLap.uid,
            samples = generateTelemetry(profile, lap.lapTime.value, lap.lapNumber.value),
          )
        }
        totalLaps += laps.size

        saved.finish(startedAt + profile.baseLapTime.times(laps.size))
        updateSessionPort.update(saved)
      }

      logger.info("Seeded ${SAMPLE_PROFILES.size} sessions and $totalLaps laps")
    }
  }

  /**
   * Synthesizes a smooth, lap-specific telemetry trace.
   *
   * Speed follows a sum of sinusoids so each track gets its own corner pattern
   * (driven by the profile's base lap time as the seed). Faster laps sit
   * uniformly slightly higher on the speed envelope; slower laps drop a touch.
   * Gear is derived from speed buckets; throttle/brake are derived from the
   * speed gradient — accelerating ⇒ throttle high, decelerating ⇒ brake high.
   */
  private fun generateTelemetry(
    profile: SeedProfile,
    lapTimeMs: Long,
    lapNumber: Int,
  ): List<TelemetrySample> {
    val samples = SAMPLES_PER_LAP
    val trackSeed = profile.baseLapTime.inWholeMilliseconds
    val cornerCount = TRACK_CORNERS[profile.track] ?: 8
    // Faster lap ⇒ slightly higher overall speed envelope.
    val paceFactor =
      1.0 + (profile.baseLapTime.inWholeMilliseconds - lapTimeMs).toDouble() / 50_000.0
    val lapJitter = (lapNumber * 17 % 7) * 0.4

    val speeds = DoubleArray(samples)
    for (i in 0 until samples) {
      val t = i.toDouble() / samples
      // Track-specific corner pattern.
      val cornerWave = sin(2.0 * PI * cornerCount * t + (trackSeed % 31) * 0.1)
      val secondaryWave = cos(4.0 * PI * t + lapJitter)
      val baseline = 180.0 + 60.0 * cornerWave + 25.0 * secondaryWave
      speeds[i] = max(60.0, baseline * paceFactor)
    }

    return List(samples) { i ->
      val splinePosition = i.toDouble() / samples
      val speed = speeds[i]
      val nextSpeed = speeds[min(samples - 1, i + 1)]
      val gradient = nextSpeed - speed
      val throttle =
        when {
          gradient > 1.0 -> min(1.0, 0.7 + gradient / 20.0)
          gradient > -0.5 -> 0.6
          else -> max(0.0, 0.3 + gradient / 30.0)
        }
      val brake =
        when {
          gradient < -2.0 -> min(1.0, -gradient / 8.0)
          else -> 0.0
        }
      val gear =
        when {
          speed < 90 -> 2
          speed < 130 -> 3
          speed < 170 -> 4
          speed < 210 -> 5
          else -> 6
        }
      TelemetrySample(
        splinePosition = splinePosition,
        speedKph = speed,
        gear = gear,
        throttle = throttle,
        brake = brake,
      )
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
    private const val SAMPLES_PER_LAP = 150

    /** Rough corner count per track, used to shape the synthetic speed trace. */
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
          Simulator.F1,
          Track("Monaco"),
          Car("F1 2026"),
          CarId(0),
          SessionType("Practice"),
          15,
          1.minutes + 12500.milliseconds,
        ),
        SeedProfile(
          Simulator.F1,
          Track("Suzuka"),
          Car("F1 2026"),
          CarId(0),
          SessionType("Race"),
          25,
          1.minutes + 31900.milliseconds,
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
