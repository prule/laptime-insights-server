package com.github.prule.laptimeinsights

import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

class SessionState {
  @OptIn(ExperimentalAtomicApi::class) private val lapCounts = mutableMapOf<Int, AtomicInt>()

  private val validLaps = mutableMapOf<Int, MutableList<ValidLap>>()
  var focussedCar: Int? = null

  @OptIn(ExperimentalAtomicApi::class)
  fun incrementLapCount(carId: Int) =
    LapNumber(lapCounts.getOrPut(carId) { AtomicInt(0) }.incrementAndFetch())

  fun isValidLap(carId: Int, lapNumber: LapNumber): Boolean {
    if (!validLaps.containsKey(carId)) return false
    if (validLaps[carId]!!.size < lapNumber.value) return false
    return validLaps[carId]!![lapNumber.value - 1].value
  }

  fun setValidLap(carId: Int, validLap: ValidLap) {
    validLaps.getOrPut(carId) { mutableListOf() }.add(validLap)
  }


}
