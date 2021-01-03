package ch.guengel.imageserver.modules

import com.google.gson.JsonSyntaxException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*

fun Application.setupExceptionHandler() {
    install(StatusPages) {
        exception<IllegalArgumentException> {
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<JsonSyntaxException> {
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}