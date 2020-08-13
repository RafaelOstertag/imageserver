package ch.guengel.imageserver.modules

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond

fun Application.setupExceptionHandler() {
    install(StatusPages) {
        exception<IllegalArgumentException> {
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}