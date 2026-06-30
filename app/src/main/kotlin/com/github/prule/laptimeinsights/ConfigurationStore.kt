package com.github.prule.laptimeinsights

import com.github.prule.laptimeinsights.adapter.out.persistence.JsonFileConfigurationRepository
import java.io.File

/**
 * Holds the live [ApplicationConfiguration] so handlers always read the current value and runtime
 * changes (e.g. toggling the public profile) are reflected without a restart. When [file] is set,
 * mutations are persisted back to it so they survive a restart; when null (tests) the store is
 * in-memory only.
 *
 * Writes are serialized so two concurrent toggles can't interleave a half-applied config.
 */
class ConfigurationStore(
  initial: ApplicationConfiguration,
  private val file: File? = null,
  private val repository: JsonFileConfigurationRepository = JsonFileConfigurationRepository(),
) {
  @Volatile
  var configuration: ApplicationConfiguration = initial
    private set

  /**
   * Turn the public profile on/off, persist the new state, and return the updated configuration.
   */
  @Synchronized
  fun setPublicProfileEnabled(enabled: Boolean): ApplicationConfiguration {
    val updated =
      configuration.copy(publicProfile = configuration.publicProfile.copy(enabled = enabled))
    file?.let { repository.saveConfiguration(updated, it) }
    configuration = updated
    return updated
  }
}
