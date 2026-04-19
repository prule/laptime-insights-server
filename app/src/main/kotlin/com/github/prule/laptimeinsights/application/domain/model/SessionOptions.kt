package com.github.prule.laptimeinsights.application.domain.model

data class SessionOptions(
    val cars: List<Car>,
    val tracks: List<Track>,
    val simulators: List<Simulator>,
)
