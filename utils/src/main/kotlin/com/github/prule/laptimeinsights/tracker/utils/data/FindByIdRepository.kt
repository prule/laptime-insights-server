package com.github.prule.laptimeinsights.tracker.utils.data

import com.github.prule.laptimeinsights.tracker.utils.NotFoundException

interface FindByIdRepository<T, ID> {
  fun findOneOrNull(id: ID): T?
}

fun <T, ID> FindByIdRepository<T, ID>.findOneOrThrow(id: ID): T =
  findOneOrNull(id) ?: throw NotFoundException()
