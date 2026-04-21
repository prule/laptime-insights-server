package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionOptions
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SessionOptionsResource(
  val cars: List<Car>,
  val tracks: List<Track>,
  val simulators: List<Simulator>,
  val minDate: Instant,
  val maxDate: Instant,
  val _links: Map<String, String>,
) {

  companion object {
    fun fromDomain(
      options: SessionOptions,
      linkFactory: SessionOptionsLinkFactory,
    ): SessionOptionsResource =
      SessionOptionsResource(
        cars = options.cars.sortedBy { it.value },
        tracks = options.tracks.sortedBy { it.value },
        simulators = options.simulators.sortedBy { it.name },
        minDate = options.from ?: Instant.DISTANT_PAST,
        maxDate = options.to ?: Instant.DISTANT_FUTURE,
        _links = linkFactory.build(options),
      )
  }
}

class SessionOptionsLinkFactory(private val application: Application) :
  LinkFactory<SessionOptions> {
  override fun build(resource: SessionOptions): Map<String, String> {
    return listOfNotNull("self" to application.href(SessionRoutes.Options())).toMap()
  }
}
