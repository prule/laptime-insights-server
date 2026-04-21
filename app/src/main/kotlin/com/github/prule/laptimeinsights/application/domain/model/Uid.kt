package com.github.prule.laptimeinsights.application.domain.model

import java.util.*
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Uid(val value: String = UUID.randomUUID().toString().replace("-", ""))
