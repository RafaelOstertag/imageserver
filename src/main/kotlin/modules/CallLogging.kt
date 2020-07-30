package ch.guengel.imageserver.modules

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import org.slf4j.event.Level

fun Application.setupCallLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}