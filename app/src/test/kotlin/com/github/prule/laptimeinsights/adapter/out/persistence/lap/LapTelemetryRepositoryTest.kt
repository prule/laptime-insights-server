package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class LapTelemetryRepositoryTest : RepositoryTest(listOf(LapTelemetryTable)) {
  private val mapper = LapTelemetryMapper()
  private val repository = LapTelemetryRepository(mapper)

  @Test
  fun `inserts a batch of samples and reads them back ordered by spline position`() {
    val lapUid = Uid()
    val samples = listOf(
      TelemetrySample(splinePosition = 0.5, speedKph = 200.0, gear = 5, throttle = 0.9, brake = 0.0),
      TelemetrySample(splinePosition = 0.0, speedKph = 80.0, gear = 2, throttle = 0.4, brake = 0.0),
      TelemetrySample(splinePosition = 1.0, speedKph = 60.0, gear = 2, throttle = 0.0, brake = 0.6),
    )

    transaction {
      repository.create(LapId(42L), lapUid, samples)
      val read = repository.findByLapUid(lapUid)

      assertThat(read).hasSize(3)
      assertThat(read.map { it.splinePosition }).containsExactly(0.0, 0.5, 1.0)
      assertThat(read[0].speedKph).isEqualTo(80.0)
      assertThat(read[2].brake).isEqualTo(0.6)
    }
  }

  @Test
  fun `findByLapUid scopes to the requested lap`() {
    val lapA = Uid()
    val lapB = Uid()

    transaction {
      repository.create(
        LapId(1L),
        lapA,
        listOf(TelemetrySample(0.0, 100.0, 3, 0.5, 0.0)),
      )
      repository.create(
        LapId(2L),
        lapB,
        listOf(
          TelemetrySample(0.0, 110.0, 3, 0.6, 0.0),
          TelemetrySample(0.5, 120.0, 4, 0.7, 0.0),
        ),
      )

      assertThat(repository.findByLapUid(lapA)).hasSize(1)
      assertThat(repository.findByLapUid(lapB)).hasSize(2)
    }
  }
}
