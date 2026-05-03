package com.github.prule.laptimeinsights

import com.github.prule.acc.client.CarModelRepository
import com.github.prule.laptimeinsights.adapter.out.event.InMemoryEventAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapTelemetryMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapTelemetryPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapTelemetryRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionRepository
import com.github.prule.laptimeinsights.application.domain.service.car.FindCarService
import com.github.prule.laptimeinsights.application.domain.service.lap.CompareLapsService
import com.github.prule.laptimeinsights.application.domain.service.lap.CreateLapService
import com.github.prule.laptimeinsights.application.domain.service.lap.FindLapService
import com.github.prule.laptimeinsights.application.domain.service.lap.FindLapTelemetryService
import com.github.prule.laptimeinsights.application.domain.service.lap.SearchLapService
import com.github.prule.laptimeinsights.application.domain.service.session.CreateSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.FindSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.FinishSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.SearchSessionOptionsService
import com.github.prule.laptimeinsights.application.domain.service.session.SearchSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.StartSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.UpdateSessionService

class AppModule {
  val eventPort = InMemoryEventAdapter()
  val session = Session()
  val lap = Lap(session)
  val car = Car()

  class Car {
    val carModelRepository = CarModelRepository()
    val findCarUseCase = FindCarService(carModelRepository)
  }

  inner class Lap(val session: Session) {
    val mapper = LapMapper()

    val lapPort = LapPersistenceAdapter(LapRepository(mapper), mapper)

    val telemetryMapper = LapTelemetryMapper()
    val telemetryPort = LapTelemetryPersistenceAdapter(LapTelemetryRepository(telemetryMapper))

    val createLapUseCase = CreateLapService(lapPort, session.sessionPort, eventPort)
    val searchLapUseCase = SearchLapService(lapPort)
    val findLapUseCase = FindLapService(lapPort)
    val findLapTelemetryUseCase = FindLapTelemetryService(lapPort, telemetryPort)
    val compareLapsUseCase = CompareLapsService(lapPort, telemetryPort)
  }

  inner class Session {

    val mapper = SessionMapper()
    val sessionPort = SessionPersistenceAdapter(SessionRepository(mapper), mapper)

    val startSessionUseCase = StartSessionService(sessionPort, sessionPort, eventPort)

    val findSessionUseCase = FindSessionService(sessionPort)
    val createSessionUseCase = CreateSessionService(sessionPort, eventPort)
    val searchSessionUseCase = SearchSessionService(sessionPort)
    val searchSessionOptionsUseCase = SearchSessionOptionsService(sessionPort)
    val finishSessionUseCase = FinishSessionService(sessionPort, sessionPort, eventPort)
    val updateSessionUseCase = UpdateSessionService(sessionPort, sessionPort, eventPort)
  }
}
