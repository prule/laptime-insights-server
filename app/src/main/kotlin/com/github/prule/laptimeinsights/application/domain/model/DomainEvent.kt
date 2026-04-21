package com.github.prule.laptimeinsights.application.domain.model

sealed interface DomainEvent

data class SessionCreated(val session: Session) : DomainEvent

data class LapCreated(val lap: Lap) : DomainEvent

data class SessionUpdated(val session: Session) : DomainEvent

data class SessionFinished(val session: Session) : DomainEvent
