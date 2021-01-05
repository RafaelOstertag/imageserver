package ch.guengel.imageserver

import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    val env = commandLineEnvironment(args)
    embeddedServer(Netty, env).start(true)
}