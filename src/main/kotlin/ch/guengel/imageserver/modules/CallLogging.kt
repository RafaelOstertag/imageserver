package ch.guengel.imageserver.modules

import io.ktor.application.*
import io.ktor.features.*
import org.slf4j.event.Level

fun Application.setupCallLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}