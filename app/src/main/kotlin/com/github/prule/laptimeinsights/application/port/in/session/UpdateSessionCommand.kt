package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlinx.serialization.Serializable

/**
 * PATCH-style update: a `null` field means "leave the existing value alone", a non-null field
 * replaces the corresponding session field.
 *
 * The driving caller is `ClientInitializer`, which builds up `track`, `car`, and `playerCarId`
 * incrementally as ACC telemetry arrives. Unknown values must NOT clobber what is already
 * persisted, hence the leave-alone semantic.
 *
 * If a future REST endpoint needs to support clearing a field explicitly, that should be modelled
 * with a sentinel/optional rather than overloading `null`.
 */
@Serializable
data class UpdateSessionCommand(
  val uid: Uid,
  val track: Track? = null,
  val car: Car? = null,
  val playerCarId: CarId? = null,
) {
  fun copyToSession(session: Session): Session =
    session.copy(
      track = track ?: session.track,
      car = car ?: session.car,
      playerCarId = playerCarId ?: session.playerCarId,
    )
}
