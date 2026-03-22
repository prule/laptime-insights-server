package io.github.prule.acc.client.utils.io.github.prule.sim.tracker.utils.io

import java.io.File
import java.io.InputStream

class FileSource(
    private val path: String,
) : Source {
    override fun inputStream(): InputStream {
        val file = File(path)
        if (!file.exists()) throw Exception("File not found: $path")
        return file.inputStream()
    }
}
