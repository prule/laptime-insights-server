package io.github.prule.acc.client.app.io.github.prule.sim.tracker.adapter.`in`.web

import io.github.prule.sim.tracker.utils.data.Order
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.Sort
import io.github.prule.sim.tracker.utils.data.SortBy
import io.ktor.server.routing.*

fun RoutingRequest.toPageRequest(): PageRequest {
  val page = queryParameters["page"]?.toIntOrNull() ?: 1
  val size = queryParameters["size"]?.toIntOrNull() ?: 25
  return PageRequest(page, size)
}

fun RoutingRequest.toSort(): Sort {
  val fields = queryParameters["sort"]?.split(",")?.map { it.split(":") }
  return if (fields != null) {
    Sort(fields.map { SortBy(it[0], Order.valueOf(it[1])) })
  } else {
    Sort.noSort()
  }
}
