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

  /**
   * Throttled telemetry frame for the player's own car (~5 Hz). Carries everything the Live
   * screen needs: gear, speed, track position, race position, current lap time, validity, delta.
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
