package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.FindSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.FindSessionUseCase
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FindSessionService(private val searchSessionPort: SearchSessionPort) : FindSessionUseCase {
  override fun findSession(command: FindSessionCommand): Session = transaction {
    searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
      ?: throw NotFoundException(command.uid.toString())
  }
}
