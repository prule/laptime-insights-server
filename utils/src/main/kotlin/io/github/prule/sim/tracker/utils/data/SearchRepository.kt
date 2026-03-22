package io.github.prule.sim.tracker.utils.data

import org.jetbrains.exposed.v1.core.Expression

interface SearchRepository<T, C> {
    fun sortableFields(): SortableFields

    fun searchForOne(
        criteria: C,
        sort: Sort? = null,
    ): T?

    fun search(
        criteria: C,
        pageRequest: PageRequest,
        sort: Sort? = null,
    ): Page<T>
}

data class SortableFields(
    private val mapping: Map<String, Expression<*>>,
)

data class Sort(
    private val fields: List<SortBy>,
)

enum class Order {
    ASC,
    DESC,
}

data class SortBy(
    private val field: String,
    private val order: Order,
)

data class Page<T>(
    val page: PageRequest,
    val total: Long,
    val items: List<T>,
)

data class PageRequest(
    val page: Int,
    val size: Int,
)
