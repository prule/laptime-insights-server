package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlinx.serialization.Serializable

@Serializable data class FindLapCommand(val uid: Uid)
