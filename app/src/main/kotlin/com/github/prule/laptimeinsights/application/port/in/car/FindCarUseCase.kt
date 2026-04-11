package com.github.prule.laptimeinsights.application.port.`in`.car

import com.github.prule.laptimeinsights.application.domain.model.Car

interface FindCarUseCase {
  fun findCarByModel(command: FindCarCommand): Car
}
