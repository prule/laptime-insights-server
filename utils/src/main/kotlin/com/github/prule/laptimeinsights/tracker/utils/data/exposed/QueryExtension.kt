package com.github.prule.laptimeinsights.tracker.utils.data.exposed

import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort
import com.github.prule.laptimeinsights.tracker.utils.data.SortableFields
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Query

fun <T> Query.paginate(
    pageRequest: PageRequest,
    sort: Sort,
    sortableFields: SortableFields,
    transform: (ResultRow) -> T,
): Page<T> {
  val total = count()

  val order =
      sort.fields
          .map {
            val col = sortableFields.mapping[it.field]!!
            col to SortOrder.valueOf(it.order.name)
          }
          .toTypedArray()

  if (order.isNotEmpty()) orderBy(*order)

  val results =
      limit(pageRequest.size).offset(((pageRequest.page - 1) * pageRequest.size).toLong()).toList()

  return Page(pageRequest, total, results.map(transform))
}

fun <T> Query.firstOrNull(
    sort: Sort,
    sortableFields: SortableFields,
    transform: (ResultRow) -> T,
): T? {

  val order =
      sort.fields
          .map {
            val col = sortableFields.mapping[it.field]!!
            col to SortOrder.valueOf(it.order.name)
          }
          .toTypedArray()

  if (order.isNotEmpty()) orderBy(*order)

  val results = limit(1).offset(0).toList()

  return results.map(transform).firstOrNull()
}
