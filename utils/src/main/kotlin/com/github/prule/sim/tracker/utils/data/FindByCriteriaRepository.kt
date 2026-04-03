package com.github.prule.sim.tracker.utils.data

import com.github.prule.sim.tracker.utils.NotFoundException

interface FindByCriteriaRepository<T> {
  fun findOneOrNull(criteria: FindCriteria<T>): T? = criteria.find()
}

fun <T> FindByCriteriaRepository<T>.findOneOrThrow(criteria: FindCriteria<T>): T =
    findOneOrNull(criteria) ?: throw NotFoundException("Not Found")

interface FindCriteria<T> {
  fun find(): T?
}
