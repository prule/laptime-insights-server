package com.github.prule.laptimeinsights

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationConfiguration(
  val port: Int = 8000,
  val clientConfiguration: ApplicationClientConfiguration = ApplicationClientConfiguration(),
)
