package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Envelope for frames sent by [SessionEventController] over the `/api/1/events` WebSocket.
 *
 * kotlinx.serialization's polymorphic encoding for sealed types adds a `"type"` discriminator field
 * to each emitted JSON object, so frames look like:
 * ```json
 * { "type": "SessionCreated", "data": { ...SessionResource... } }
 * { "type": "LapCreated",     "data": { ...LapResource... } }
 * ```
 *
 * Clients distinguish messages by inspecting `type` rather than guessing from field shape. The
 * `@SerialName` value on each subclass is the stable wire identifier — do not rename without
 * coordinating with frontend clients, as it is part of the public protocol.
 *
 * Send via `sendSerialized<WebSocketMessage>(...)` so the polymorphic serializer fires.
 */
@Serializable
sealed interface WebSocketMessage {
  @Serializable
  @SerialName("SessionCreated")
  data class SessionCreated(val data: SessionResource) : WebSocketMessage

  @Serializable
  @SerialName("SessionStarted")
  data class SessionStarted(val data: SessionResource) : WebSocketMessage

  @Serializable
  @SerialName("SessionUpdated")
  data class SessionUpdated(val data: SessionResource) : WebSocketMessage

  @Serializable
  @SerialName("SessionFinished")
  data class SessionFinished(val data: SessionResource) : WebSocketMessage

  @Serializable
  @SerialName("LapCreated")
  data class LapCreated(val data: LapResource) : WebSocketMessage
}
