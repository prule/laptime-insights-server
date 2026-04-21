package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CreateLapCommand(
    val sessionUid: Uid,
    val recordedAt: Instant,
    val carId: CarId,
    val lapTime: LapTimeMs,
    val lapNumber: LapNumber,
    val valid: ValidLap,
    val personalBest: PersonalBest,
)
