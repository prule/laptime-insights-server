package io.github.prule.sim.tracker.utils.data

import io.github.prule.sim.tracker.utils.NotFoundException

interface FindByIdRepository<T, ID> {
    fun findOneOrNull(id: ID): T?
}

fun <T, ID> FindByIdRepository<T, ID>.findOneOrThrow(id: ID): T = findOneOrNull(id) ?: throw NotFoundException()
