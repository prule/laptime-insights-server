package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample

class LapTelemetryMapper {
  fun toDomain(entity: LapTelemetryEntity): TelemetrySample =
    TelemetrySample(
      splinePosition = entity.splinePosition,
      speedKph = entity.speedKph,
      gear = entity.gear,
      throttle = entity.throttle,
      brake = entity.brake,
    )
}
