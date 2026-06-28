package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Uid

/**
 * Inbound port invoked when a car model becomes known (its `EntryListCar` is processed), to
 * attribute that model to laps already recorded car-less for the same session + car index.
 */
interface RecordCarOnLapsUseCase {
  /** Back-fill [car] onto the car-less laps of [sessionUid] + [carIndex]. Returns the row count. */
  fun fillMissingCar(sessionUid: Uid, carIndex: CarId, car: Car): Int
}
