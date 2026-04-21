package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlinx.serialization.Serializable

@Serializable
data class UpdateSessionCommand(val uid: Uid, val track: Track? = null, val car: Car? = null) {
  fun copyToSession(session: Session): Session {
    return session.copy(
      track = track.let { if (it == session.track) null else it },
      car = car.let { if (it == session.car) null else it },
    )
  }
}
