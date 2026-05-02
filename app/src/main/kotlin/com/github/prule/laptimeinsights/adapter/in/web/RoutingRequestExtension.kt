package com.github.prule.laptimeinsights.adapter.`in`.web

import com.github.prule.laptimeinsights.tracker.utils.data.Order
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort
import com.github.prule.laptimeinsights.tracker.utils.data.SortBy
import io.ktor.server.routing.RoutingRequest

fun RoutingRequest.toPageRequest(): PageRequest {
  val page = queryParameters["page"]?.toIntOrNull() ?: 1
  val size = queryParameters["size"]?.toIntOrNull() ?: 25
  return PageRequest(page, size)
}

fun RoutingRequest.toSort(): Sort {
  // `?sort=field:DIR` (single) or `?sort=field:DIR,field:DIR` (multi). Direction is
  // case-insensitive so clients can send `desc` or `DESC`.
  val raw = queryParameters["sort"] ?: return Sort.noSort()
  val parts =
    raw
      .split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .map {
        val (field, direction) = it.split(":", limit = 2).let { p ->
          if (p.size == 2) p[0].trim() to p[1].trim() else p[0].trim() to "ASC"
        }
        SortBy(field, Order.valueOf(direction.uppercase()))
      }
  return if (parts.isEmpty()) Sort.noSort() else Sort(parts)
}
