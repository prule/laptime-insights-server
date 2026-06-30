package com.github.prule.laptimeinsights.application.port.out.profile

import com.github.prule.laptimeinsights.application.domain.model.ProfileAggregates

/** Reads the local Session/Lap aggregates needed to build the public profile snapshot. */
interface ProfileAggregatesPort {
  fun loadAggregates(): ProfileAggregates
}
