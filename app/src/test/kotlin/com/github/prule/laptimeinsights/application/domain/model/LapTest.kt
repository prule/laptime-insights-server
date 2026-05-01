package com.github.prule.laptimeinsights.application.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LapTest {

  @ParameterizedTest
  @CsvSource("01:36.745, 96745", "00:01.000, 1000", "10:00.000, 600000", "01:00.000, 60000")
  fun `should parse lap time from string`(input: String, expectedMs: Long) {
    val lapTime = LapTimeMs.fromString(input)
    assertThat(lapTime.value).isEqualTo(expectedMs)
  }

  @Test
  fun `should throw error for invalid lap time format`() {
    assertThatThrownBy { LapTimeMs.fromString("invalid") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("Invalid lap time: invalid")
  }
}
