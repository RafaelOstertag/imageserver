package ch.guengel.imageserver.modules

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
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