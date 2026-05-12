package com.github.prule.laptimeinsights

/**
 * High-level features the frontend can toggle navigation/UI around. Each entry has a stable [rel]
 * (the HATEOAS link relation name surfaced by `GET /api/1`) and an [envVar] that disables it when
 * set to a falsy value. All features default to enabled.
 */
enum class Feature(val rel: String, val envVar: String) {
  OVERVIEW("overview", "FEATURE_OVERVIEW"),
  SESSIONS("sessions", "FEATURE_SESSIONS"),
  LAPS("laps", "FEATURE_LAPS"),
  COMPARE("compare", "FEATURE_COMPARE"),
  LIVE("live", "FEATURE_LIVE"),
}
