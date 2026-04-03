package com.github.prule.laptimeinsights.adapter.out.persistence

import com.github.prule.laptimeinsights.ApplicationConfiguration
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import io.github.xn32.json5k.Json5
import java.io.File

class JsonFileConfigurationRepository {
  val json = Json5

  fun loadConfiguration(path: String): ApplicationConfiguration {
    return loadConfiguration(File(path))
  }

  fun loadConfiguration(file: File): ApplicationConfiguration {
    if (!file.exists()) {
      throw NotFoundException("File $file does not exist")
    }
    return json.decodeFromString(ApplicationConfiguration.serializer(), file.readText())
  }
}
