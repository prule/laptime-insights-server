package com.github.prule.laptimeinsights.adapter.out.persistence.profile

import com.github.prule.laptimeinsights.adapter.out.persistence.TimeBucketUnit
import com.github.prule.laptimeinsights.adapter.out.persistence.dateTrunc
import com.github.prule.laptimeinsights.adapter.out.persistence.formatTimeBucketKey
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapTable
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionTable
import com.github.prule.laptimeinsights.application.domain.model.DayLaps
import com.github.prule.laptimeinsights.application.domain.model.ProfileAggregates
import com.github.prule.laptimeinsights.application.domain.model.RecordAggregate
import com.github.prule.laptimeinsights.application.domain.model.TrackLaps
import com.github.prule.laptimeinsights.application.port.out.profile.ProfileAggregatesPort
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Reads the public-profile aggregates from the local Session/Lap tables. All figures are over the
 * **player's** laps (`LAP.player_lap = true`). Records fold valid player laps in memory to derive,
 * per (track, car), the all-time best (+ its date) and the current-year season best — single-user
 * local volumes make this cheap and avoids window-function SQL.
 *
 * Must be called inside an Exposed transaction (the service wraps it).
 */
class ProfilePersistenceAdapter : ProfileAggregatesPort {

  override fun loadAggregates(): ProfileAggregates {
    val playerLap = LapTable.playerLap eq true
    val lapCount = LapTable.id.count()

    val playerLapCount = LapTable.selectAll().andWhere { playerLap }.count()

    val perTrackLaps =
      LapTable.selectAll()
        .andWhere { playerLap and LapTable.track.isNotNull() }
        .adjustSelect { select(LapTable.track, lapCount) }
        .groupBy(LapTable.track)
        .orderBy(lapCount, SortOrder.DESC)
        .map { TrackLaps(track = it[LapTable.track]!!, laps = it[lapCount]) }

    val perCarLaps =
      LapTable.selectAll()
        .andWhere { playerLap and LapTable.car.isNotNull() }
        .adjustSelect { select(LapTable.car, lapCount) }
        .groupBy(LapTable.car)
        .map { it[LapTable.car]!! to it[lapCount] }

    val activeDays = run {
      val dayTrunc = dateTrunc(TimeBucketUnit.DAY, LapTable.recordedAt)
      LapTable.selectAll()
        .andWhere { playerLap }
        .adjustSelect { select(dayTrunc, lapCount) }
        .groupBy(dayTrunc)
        .map { DayLaps(formatTimeBucketKey(it[dayTrunc], TimeBucketUnit.DAY), it[lapCount]) }
        .sortedBy { it.dateKey }
    }

    val sessionCount = SessionTable.selectAll().count()
    val seatTimeMs = run {
      val sumExpr = SessionTable.drivingTimeMs.sum()
      SessionTable.selectAll().adjustSelect { select(sumExpr) }.first()[sumExpr] ?: 0L
    }

    val (firstRecordedAt, lastRecordedAt) =
      run {
        val minExpr = LapTable.recordedAt.min()
        val maxExpr = LapTable.recordedAt.max()
        val row =
          LapTable.selectAll()
            .andWhere { playerLap }
            .adjustSelect { select(minExpr, maxExpr) }
            .first()
        row[minExpr] to row[maxExpr]
      }

    return ProfileAggregates(
      playerLapCount = playerLapCount,
      sessionCount = sessionCount,
      seatTimeMs = seatTimeMs,
      trackCount = perTrackLaps.size,
      carCount = perCarLaps.size,
      topCar = perCarLaps.maxByOrNull { it.second }?.first,
      perTrackLaps = perTrackLaps,
      activeDays = activeDays,
      records = loadRecords(),
      firstRecordedAt = firstRecordedAt,
      lastRecordedAt = lastRecordedAt,
    )
  }

  private fun loadRecords(): List<RecordAggregate> {
    val seasonStart =
      LocalDate.now()
        .withDayOfYear(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toKotlinInstant()

    class Acc(var allBest: Long, var allWhen: Instant, var seasonBest: Long?)
    val byCombo = LinkedHashMap<Pair<String, String>, Acc>()

    LapTable.selectAll()
      .andWhere {
        (LapTable.playerLap eq true) and
          (LapTable.valid eq true) and
          LapTable.track.isNotNull() and
          LapTable.car.isNotNull()
      }
      .adjustSelect { select(LapTable.track, LapTable.car, LapTable.lapTime, LapTable.recordedAt) }
      .forEach { row ->
        val key = row[LapTable.track]!! to row[LapTable.car]!!
        val lapTime = row[LapTable.lapTime]
        val recordedAt = row[LapTable.recordedAt]
        val acc = byCombo.getOrPut(key) { Acc(lapTime, recordedAt, null) }
        if (lapTime < acc.allBest) {
          acc.allBest = lapTime
          acc.allWhen = recordedAt
        }
        if (recordedAt >= seasonStart && (acc.seasonBest == null || lapTime < acc.seasonBest!!)) {
          acc.seasonBest = lapTime
        }
      }

    return byCombo
      .map { (key, acc) ->
        RecordAggregate(
          track = key.first,
          car = key.second,
          seasonBestMs = acc.seasonBest,
          allTimeBestMs = acc.allBest,
          allTimeWhen = acc.allWhen,
        )
      }
      .sortedWith(compareBy({ it.track }, { it.car }))
  }
}
