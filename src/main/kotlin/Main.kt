package ch.guengel.imageserver

import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val env = commandLineEnvironment(args)
    embeddedServer(Netty, env).start(true)
}