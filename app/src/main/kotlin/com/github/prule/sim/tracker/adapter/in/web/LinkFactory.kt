package com.github.prule.sim.tracker.adapter.`in`.web

interface LinkFactory<T> {
  fun build(resource: T): Map<String, String>
}
