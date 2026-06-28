package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.RecordCarOnLapsUseCase
import com.github.prule.laptimeinsights.application.port.out.lap.RecordCarOnLapsPort

/**
 * Attributes a resolved car model to laps that were recorded before the car was known, so a lap
 * that completed ahead of its `EntryListCar` no longer keeps a null car.
 */
class RecordCarOnLapsService(private val port: RecordCarOnLapsPort) : RecordCarOnLapsUseCase {
  override fun fillMissingCar(sessionUid: Uid, carIndex: CarId, car: Car): Int =
    port.fillMissingCar(sessionUid, carIndex, car)
}
