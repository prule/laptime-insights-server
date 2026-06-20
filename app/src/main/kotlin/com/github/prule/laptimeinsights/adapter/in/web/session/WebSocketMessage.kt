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
  /**
   * Sent immediately when a client establishes a WebSocket connection, before the event stream
   * begins. Clients should reset all live state on receipt — this frame signals that the server is
   * fresh and any prior in-memory state (active session, laps) no longer exists.
   */
  @Serializable @SerialName("ServerStarted") data object ServerStarted : WebSocketMessage

  @Serializable
  @SerialName("SessionCreated")
  data class SessionCreated(val data: SessionResource) : WebSocketMessage

  @Serializable
  @SerialName("SessionStarted")
  data class SessionStarted(val data: SessionResource) : WebSocketMessage

  @Serializable
  @SerialName("SessionUpdated")
  data class SessionUpdated(val data: SessionResource) : WebSocketMessage

  /**
   * Sent when a session is finalized — a new ACC session identity was detected or a terminal
   * session phase was reached (see `SessionTracker`). The carried [SessionResource] has a populated
   * `endedAt`. Clients should treat this as the signal the session is no longer "live".
   */
  @Serializable
  @SerialName("SessionEnded")
  data class SessionEnded(val data: SessionResource) : WebSocketMessage

  @Serializable
  @SerialName("LapCreated")
  data class LapCreated(val data: LapResource) : WebSocketMessage

  /**
   * Throttled telemetry frame for the player's own car (~5 Hz). Carries everything the Live screen
   * needs: gear, speed, track position, race position, current lap time, validity, delta.
   *
   * Note: ACC's REALTIME_CAR_UPDATE does **not** include throttle or brake inputs — those fields
   * are not available and are therefore absent from this message.
   */
  @Serializable
  @SerialName("PlayerCarUpdated")
  data class PlayerCarUpdated(val data: PlayerCarUpdateData) : WebSocketMessage
}

/**
 * Payload for [WebSocketMessage.PlayerCarUpdated]. All lap times are in milliseconds.
 * [bestLapTimeMs] and [lastLapTimeMs] are [Long.MAX_VALUE] when no timed lap exists yet.
 */
@Serializable
data class PlayerCarUpdateData(
  val sessionUid: String,
  val gear: Int,
  val kmh: Int,
  val splinePosition: Double,
  val worldPosX: Float,
  val worldPosY: Float,
  val racePosition: Int,
  val currentLapTimeMs: Long,
  val currentLapIsInvalid: Boolean,
  val delta: Int,
  val bestLapTimeMs: Long,
  val lastLapTimeMs: Long,
)
