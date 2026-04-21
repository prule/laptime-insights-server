package com.github.prule.laptimeinsights.simulator

import com.github.prule.acc.client.simulator.AccSimulator
import com.github.prule.acc.client.simulator.AccSimulatorConfiguration
import com.github.prule.acc.client.simulator.FileSource
import com.github.prule.acc.client.simulator.Source
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
  println("Starting ACC Simulator...")
  val eventsArg = args.find { it.startsWith("--events=") }
  val source: Source =
    if (eventsArg != null) {
      val path = eventsArg.substringAfter("=")
      println("Using custom events file: $path")
      FileSource(path)
    } else {
      println("Using default classpath events file.")
      FileSource(
        "./recordings/10min-race-snetterton-ferrari-simulator-recording-2026-04-11T20-04-49.434542=abbrev.csv"
      )
    }

  AccSimulator(
      AccSimulatorConfiguration(
        port = 9000,
        connectionPassword = "asd",
        playbackEventsFile = source,
      )
    )
    .start()
}
