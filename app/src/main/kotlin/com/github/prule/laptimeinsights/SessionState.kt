package com.github.prule.laptimeinsights

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * Mutable per-session state shared between the message listeners in [ClientInitializer].
 *
 * Lap counts are incremented atomically on each [LAPCOMPLETED] event.
 *
 * Lap validity is tracked by recording the latest [currentLapIsInvalid] flag from each
 * [REALTIME_CAR_UPDATE] message. ACC resets the flag to 0 (valid) at the start of every
 * new lap and sets it to 1 if a track-limits violation occurs during that lap. Reading the
 * flag at [LAPCOMPLETED] therefore reflects whether the just-finished lap was clean.
 */
class SessionState {
  @OptIn(ExperimentalAtomicApi::class) private val lapCounts = mutableMapOf<CarId, AtomicInt>()

  /** Latest `currentLapIsInvalid` value seen for each car. Default: valid (true). */
  private val currentLapValid = mutableMapOf<CarId, Boolean>()

  /** Car model name keyed by ACC car index, populated from EntryListCar messages. */
  private val carModels = mutableMapOf<CarId, Car>()

  /** Called when an [EntryListCar] message arrives for any car. */
  fun registerCar(carId: CarId, car: Car) {
    carModels[carId] = car
  }

  /** Returns the car model for [carId], or null if no EntryListCar has arrived yet. */
  fun getCarModel(carId: CarId): Car? = carModels[carId]

  @OptIn(ExperimentalAtomicApi::class)
  fun incrementLapCount(carId: CarId) =
    LapNumber(lapCounts.getOrPut(carId) { AtomicInt(0) }.incrementAndFetch())

  /**
   * Called on every [REALTIME_CAR_UPDATE]. Records whether the car's current lap is still
   * valid so [isValidLap] can read the correct value when the lap completes.
   */
  fun updateCurrentLapValidity(carId: CarId, isValid: Boolean) {
    currentLapValid[carId] = isValid
  }

  /**
   * Returns the validity of the lap that just completed for [carId]. If no [REALTIME_CAR_UPDATE]
   * has been received yet for this car we default to valid — a missing update is not a violation.
   */
  fun isValidLap(carId: CarId, @Suppress("UNUSED_PARAMETER") lapNumber: LapNumber): Boolean =
    currentLapValid[carId] ?: true
}
