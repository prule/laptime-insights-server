package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlinx.serialization.Serializable

@Serializable data class FindSessionCommand(val uid: Uid)
