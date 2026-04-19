package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionOptions
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.SearchSessionOptionsUseCase
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SearchSessionOptionsService(
    private val searchSessionPort: SearchSessionPort,
) : SearchSessionOptionsUseCase {
  override fun options(
      criteria: SessionSearchCriteria,
  ): SessionOptions = transaction { searchSessionPort.options(criteria) }
}
