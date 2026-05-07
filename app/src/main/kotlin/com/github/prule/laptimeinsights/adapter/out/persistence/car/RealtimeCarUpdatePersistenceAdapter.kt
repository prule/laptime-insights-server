package com.github.prule.laptimeinsights.adapter.out.persistence.car

import com.github.prule.laptimeinsights.application.domain.model.RealtimeCarUpdate
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.out.car.CreateRealtimeCarUpdatePort
import com.github.prule.laptimeinsights.application.port.out.car.FindRealtimeCarUpdateByLapPort
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class RealtimeCarUpdatePersistenceAdapter(private val repository: RealtimeCarUpdateRepository) :
  CreateRealtimeCarUpdatePort, FindRealtimeCarUpdateByLapPort {

  override fun create(update: RealtimeCarUpdate) {
    transaction { repository.create(update) }
  }

  override fun batchCreate(updates: List<RealtimeCarUpdate>) {
    if (updates.isEmpty()) return
    transaction { repository.batchCreate(updates) }
  }

  override fun findByLapUid(lapUid: Uid): List<TelemetrySample> = transaction {
    repository.findByLapUid(lapUid)
  }
}
