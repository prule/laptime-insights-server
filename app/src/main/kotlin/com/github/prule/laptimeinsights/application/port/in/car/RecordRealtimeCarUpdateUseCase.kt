package com.github.prule.laptimeinsights.application.port.`in`.car

interface RecordRealtimeCarUpdateUseCase {
  fun record(command: RecordRealtimeCarUpdateCommand)
}
