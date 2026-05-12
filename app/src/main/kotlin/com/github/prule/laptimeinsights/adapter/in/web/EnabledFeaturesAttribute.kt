package com.github.prule.laptimeinsights.adapter.`in`.web

import com.github.prule.laptimeinsights.Feature
import io.ktor.server.application.Application
import io.ktor.util.AttributeKey

/**
 * Carries the set of enabled [Feature]s through the Ktor application so link factories can omit
 * cross-feature rels for features the operator has turned off. Stored as an attribute (rather
 * than passed through every controller / factory constructor) because every controller already
 * receives [Application] and every link factory already takes it as an `Application` field.
 *
 * Set once in `Application.module(...)`; read by `SessionLinkFactory`, `LapLinkFactory`, and
 * `SessionOptionsLinkFactory`.
 */
private val EnabledFeaturesKey = AttributeKey<Set<Feature>>("EnabledFeatures")

fun Application.setEnabledFeatures(features: Set<Feature>) {
  attributes.put(EnabledFeaturesKey, features)
}

fun Application.enabledFeatures(): Set<Feature> =
  if (attributes.contains(EnabledFeaturesKey)) attributes[EnabledFeaturesKey] else emptySet()
