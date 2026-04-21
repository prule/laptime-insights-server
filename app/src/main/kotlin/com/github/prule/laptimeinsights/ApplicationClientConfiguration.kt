package com.github.prule.laptimeinsights

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationClientConfiguration(val port: Int = 9000, val serverIp: String = "127.0.0.1")
