package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.SessionOptions
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria

interface SearchSessionOptionsUseCase {
  fun options(criteria: SessionSearchCriteria): SessionOptions
}
