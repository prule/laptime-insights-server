package com.github.prule.laptimeinsights.adapter.out.persistence

import com.github.prule.laptimeinsights.ApplicationConfiguration
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import io.github.xn32.json5k.Json5
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

  /**
   * Persist [configuration] back to [file]. Writes to a sibling temp file then atomically renames
   * over the target so a crash mid-write can never leave a half-written config behind.
   */
  fun saveConfiguration(configuration: ApplicationConfiguration, file: File) {
    val text = json.encodeToString(ApplicationConfiguration.serializer(), configuration)
    val temp = File.createTempFile(file.name, ".tmp", file.absoluteFile.parentFile)
    temp.writeText(text)
    Files.move(
      temp.toPath(),
      file.toPath(),
      StandardCopyOption.REPLACE_EXISTING,
      StandardCopyOption.ATOMIC_MOVE,
    )
  }
}
