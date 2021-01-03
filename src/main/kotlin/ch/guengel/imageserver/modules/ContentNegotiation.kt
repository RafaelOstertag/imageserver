package ch.guengel.imageserver.modules

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import java.text.DateFormat

fun Application.setupContentNegotiation() {
    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }
}