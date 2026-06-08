package com.github.prule.laptimeinsights.tracker.utils.data

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Expression

interface SearchRepository<T, C> {

  fun searchForOne(criteria: C, sort: Sort = Sort.noSort()): T?

  fun search(criteria: C, pageRequest: PageRequest, sort: Sort = Sort.noSort()): Page<T>
}

data class SortableFields(val mapping: Map<String, Expression<*>>)

@Serializable
data class Sort(val fields: List<SortBy>) {
  companion object {
    fun noSort(): Sort {
      return Sort(emptyList())
    }
  }
}

@Serializable
enum class Order {
  ASC,
  DESC,
}

@Serializable data class SortBy(val field: String, val order: Order)

@Serializable
data class Page<T>(
  val page: PageRequest,
  val total: Long,
  val items: List<T>,
  /**
   * Field names a client may pass in the `sort` query parameter for this collection. Empty when
   * the endpoint does not advertise sorting. Surfaced so UI tables can enable/disable column
   * headers without hard-coding which fields are sortable.
   */
  val sortable: List<String> = emptyList(),
) {
  fun <R> map(block: (T) -> R): Page<R> = Page(page, total, items.map(block), sortable)

  fun withSortable(fields: List<String>): Page<T> = copy(sortable = fields)
}

@Serializable data class PageRequest(val page: Int = 1, val size: Int = 25)
