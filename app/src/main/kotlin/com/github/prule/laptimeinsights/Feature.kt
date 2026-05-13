package com.github.prule.laptimeinsights

/**
 * High-level features the frontend can toggle navigation/UI around.
 *
 * - [rel] is the stable HATEOAS link relation surfaced by `GET /api/1`.
 * - [envVar] is the override switch operators flip at deploy time.
 * - [defaultEnabled] is the in-code default applied when the env var is unset. Use this for
 *   features that are still under development (start `false`, flip on per environment) or features
 *   that should be opt-in (e.g. dev-only tooling).
 *
 * Resolution order is: env var if present and parseable → otherwise [defaultEnabled].
 */
enum class Feature(val rel: String, val envVar: String, val defaultEnabled: Boolean = true) {
  OVERVIEW("overview", "FEATURE_OVERVIEW"),
  SESSIONS("sessions", "FEATURE_SESSIONS"),
  LAPS("laps", "FEATURE_LAPS"),
  COMPARE("compare", "FEATURE_COMPARE"),
  LIVE("live", "FEATURE_LIVE"),
}
