package com.github.prule.laptimeinsights.application.domain.service.car

import com.github.prule.acc.client.CarModel
import com.github.prule.acc.client.CarModelRepository
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.port.`in`.car.FindCarCommand
import com.github.prule.laptimeinsights.application.port.`in`.car.FindCarUseCase

class FindCarService(private val carModelRepository: CarModelRepository) : FindCarUseCase {

  override fun findCarByModel(command: FindCarCommand): Car {
    val carModel =
      carModelRepository.findById(command.carModelId) ?: CarModel(command.carModelId, "Unknown")
    return Car(carModel.name)
  }
}
