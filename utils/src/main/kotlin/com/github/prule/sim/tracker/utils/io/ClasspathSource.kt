package com.github.prule.acc.client.utils.com.github.prule.sim.tracker.utils.io

import java.io.InputStream

class ClasspathSource(
    private val path: String,
) : Source {
  override fun inputStream(): InputStream =
      javaClass.classLoader.getResourceAsStream(path)
          ?: throw Exception("Resource not found on classpath: $path")
}
