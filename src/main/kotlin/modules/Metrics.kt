package ch.guengel.imageserver.modules

import io.ktor.application.*
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Application.setupMetrics() {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        this.registry = registry
    }

    routing {
        get("/metrics") {
            val result = registry.scrape()
            call.respond(result)
        }
    }
}