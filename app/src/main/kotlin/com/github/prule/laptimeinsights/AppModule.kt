package com.github.prule.laptimeinsights

import com.github.prule.acc.client.CarModelRepository
import com.github.prule.laptimeinsights.adapter.out.event.InMemoryEventAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionRepository
import com.github.prule.laptimeinsights.application.domain.service.car.FindCarService
import com.github.prule.laptimeinsights.application.domain.service.lap.CreateLapService
import com.github.prule.laptimeinsights.application.domain.service.lap.SearchLapService
import com.github.prule.laptimeinsights.application.domain.service.session.CreateSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.FindSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.FinishSessionService
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

    val lapPort =
        LapPersistenceAdapter(
            LapRepository(mapper),
            mapper,
        )

    val createLapUseCase =
        CreateLapService(
            lapPort,
            session.sessionPort,
            eventPort
        )
    val searchLapUseCase = SearchLapService(lapPort)
  }

  inner class Session {

    val mapper = SessionMapper()
    val sessionPort =
        SessionPersistenceAdapter(
            SessionRepository(mapper),
            mapper,
        )

    val startSessionUseCase =
        StartSessionService(
            sessionPort,
            sessionPort,
        )

    val findSessionUseCase = FindSessionService(sessionPort)
    val createSessionUseCase = CreateSessionService(sessionPort, eventPort)
    val searchSessionUseCase = SearchSessionService(sessionPort)
    val finishSessionUseCase =
        FinishSessionService(
            sessionPort,
            sessionPort,
        )
    val updateSessionUseCase = UpdateSessionService(sessionPort, sessionPort)
  }
}
