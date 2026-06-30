package com.github.prule.laptimeinsights.application.port.`in`.profile

import com.github.prule.laptimeinsights.PublicProfileConfig
import com.github.prule.laptimeinsights.application.domain.model.ProfileSnapshot

/** Builds the public profile snapshot from local data merged with signup identity. */
interface BuildProfileSnapshotUseCase {
  fun build(identity: PublicProfileConfig): ProfileSnapshot
}
