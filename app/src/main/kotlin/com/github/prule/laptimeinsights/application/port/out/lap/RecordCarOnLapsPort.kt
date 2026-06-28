package com.github.prule.laptimeinsights.application.port.out.lap

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Uid

/**
 * Outbound port for back-filling the car model onto laps that were recorded before the car was
 * known.
 *
 * A lap can complete before its car's `EntryListCar` message has been processed, so it is persisted
 * with a null `car`. Once the car model is resolved, this port stamps it onto the still-car-less
 * laps for that session + car index.
 */
interface RecordCarOnLapsPort {
  /**
   * Set [car] on every lap of [sessionUid] + [carIndex] whose car is null. Returns the row count.
   */
  fun fillMissingCar(sessionUid: Uid, carIndex: CarId, car: Car): Int
}
