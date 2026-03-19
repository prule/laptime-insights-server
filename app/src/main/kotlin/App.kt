package io.github.prule.acc.client.app

import io.github.prule.acc.client.utils.Printer
import org.jetbrains.exposed.v1.jdbc.Database

// TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
}
