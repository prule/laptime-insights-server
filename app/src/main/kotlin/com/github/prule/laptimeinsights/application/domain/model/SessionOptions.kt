package com.github.prule.laptimeinsights.application.domain.model

import kotlin.time.Instant

data class SessionOptions(
  val cars: List<Car>,
  val tracks: List<Track>,
  val simulators: List<Simulator>,
  val from: Instant?,
  val to: Instant?,
)
