package com.github.prule.sim.tracker.adapter.`in`.web

import com.github.prule.sim.tracker.utils.data.Order
import com.github.prule.sim.tracker.utils.data.PageRequest
import com.github.prule.sim.tracker.utils.data.Sort
import com.github.prule.sim.tracker.utils.data.SortBy
import io.ktor.server.routing.RoutingRequest

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
