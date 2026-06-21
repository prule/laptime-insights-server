package com.github.prule.laptimeinsights.tracker.utils.data.exposed

import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort
import com.github.prule.laptimeinsights.tracker.utils.data.SortableFields
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Query
import org.slf4j.LoggerFactory

private val sortLogger = LoggerFactory.getLogger("com.github.prule.laptimeinsights.tracker.utils.data.exposed.Sort")

/**
 * Resolves the requested [sort] fields against [sortableFields]. Unknown field names are skipped
 * (forward-compatibility) and logged at DEBUG so misuse is discoverable. Single-result and
 * paginated queries share this so their behaviour can never drift apart.
 */
private fun resolveOrder(
  sort: Sort,
  sortableFields: SortableFields,
): Array<Pair<Expression<*>, SortOrder>> =
  sort.fields
    .mapNotNull { sortBy ->
      val col = sortableFields.mapping[sortBy.field]
      if (col == null) {
        sortLogger.debug("Dropping unknown sort field '{}'", sortBy.field)
        null
      } else {
        col to SortOrder.valueOf(sortBy.order.name)
      }
    }
    .toTypedArray()

fun <T> Query.paginate(
  pageRequest: PageRequest,
  sort: Sort,
  sortableFields: SortableFields,
  transform: (ResultRow) -> T,
): Page<T> {
  val total = count()

  val order = resolveOrder(sort, sortableFields)
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
  val order = resolveOrder(sort, sortableFields)
  if (order.isNotEmpty()) orderBy(*order)

  val results = limit(1).offset(0).toList()

  return results.map(transform).firstOrNull()
}
