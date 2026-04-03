package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlinx.serialization.Serializable

@Serializable
data class LapResource(
    val uid: Uid,
    val sessionUid: Uid,
    val recordedAt: String,
    val lapTime: LapTimeMs,
    val lapNumber: LapNumber,
    val valid: ValidLap,
    val personalBest: PersonalBest,
    val _links: Map<String, String>,
) {

  companion object {
    fun fromDomain(lap: Lap, linkFactory: LapLinkFactory): LapResource =
        LapResource(
            uid = lap.uid,
            sessionUid = lap.sessionUId,
            recordedAt = lap.recordedAt.toString(),
            lapTime = lap.lapTime,
            lapNumber = lap.lapNumber,
            valid = lap.valid,
            personalBest = lap.personalBest,
            _links = linkFactory.build(lap),
        )
  }
}

class LapLinkFactory(private val application: Application) : LinkFactory<Lap> {
  override fun build(resource: Lap): Map<String, String> {
    val lap = LapRoutes.LapId(uid = resource.uid.value)
    return listOfNotNull(
            "self" to application.href(lap),
        )
        .toMap()
  }
}
