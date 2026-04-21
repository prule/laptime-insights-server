package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapPort
import com.github.prule.laptimeinsights.application.port.out.lap.SearchLapPort
import com.github.prule.laptimeinsights.application.port.out.lap.UpdateLapPort
import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort

class LapPersistenceAdapter(private val repository: LapRepository, private val mapper: LapMapper) :
  SearchLapPort, CreateLapPort, UpdateLapPort {
  override fun search(
    criteria: LapSearchCriteria,
    pageRequest: PageRequest,
    sort: Sort,
  ): Page<Lap> {
    return repository.search(criteria, pageRequest, sort).map(mapper::toDomain)
  }

  override fun searchForOne(criteria: LapSearchCriteria, sort: Sort): Lap? {
    return repository.searchForOne(criteria, sort)?.let(mapper::toDomain)
  }

  override fun create(lap: Lap): Lap {
    val entity = repository.create(lap)
    return mapper.toDomain(entity)
  }

  override fun update(lap: Lap): Lap {
    val entity = repository.update(lap)
    return mapper.toDomain(entity)
  }
}
