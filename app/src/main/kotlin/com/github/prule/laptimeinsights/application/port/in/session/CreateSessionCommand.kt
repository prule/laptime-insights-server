package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionCommand(
  val simulator: Simulator,
  val sessionType: SessionType,
  val track: Track? = null,
  val car: Car? = null,
)
