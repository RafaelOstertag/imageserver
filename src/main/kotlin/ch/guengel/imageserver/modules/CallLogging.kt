package ch.guengel.imageserver.modules

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import org.slf4j.event.Level

fun Application.setupCallLogging() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val path = call.request.uri
            val method = call.request.httpMethod.value
            val status = call.response.status()
            val host = call.request.host()
            val realIp = call.request.headers["X-Real-IP"]
            val forwardedFor = call.request.headers["X-Forwarded-For"]
            "[$host] $method $path $status (X-Real-IP: $realIp;X-Forwarded-For: $forwardedFor)"
        }
    }
}