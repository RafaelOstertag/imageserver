package ch.guengel.imageserver.modules

import ch.guengel.imageserver.image.ImageService
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondOutputStream
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.put
import io.ktor.routing.routing
import java.nio.file.Path
import java.util.regex.PatternSyntaxException

fun Application.imageRoute() {
    val imageDirectory = environment.config.property("images.directory").getString()
    log.info("Read images from '{}'", imageDirectory)


    val imageService = ImageService(Path.of(imageDirectory))
    routing {
        get("/images/{width}/{height}") {
            val width = call.parameters["width"]?.toInt() ?: throw IllegalArgumentException("Missing image width")
            val height =
                call.parameters["height"]?.toInt() ?: throw IllegalArgumentException("Missing image height")

            call.respondOutputStream(ContentType.Image.JPEG, HttpStatusCode.OK) {
                val image = imageService.getRandomImage(width, height)
                image.write(this)
            }
        }

        put("/images") {
            call.parameters["update"] ?: throw java.lang.IllegalArgumentException("missing update query")
            imageService.readAll()
            call.respond(HttpStatusCode.NoContent)
        }

        put("/images/exclusion") {
            val exclusionPattern = call.receive<ExclusionPattern>()
            try {
                imageService.setExclusionPattern(exclusionPattern.pattern)
            } catch (e: PatternSyntaxException) {
                throw IllegalArgumentException("Pattern is invalid")
            }
            call.respond(HttpStatusCode.NoContent)
        }

        get("/images/exclusion") {
            call.respond(ExclusionPattern(imageService.getExclusionPattern()))
        }

        delete("/images/exclusion") {
            imageService.resetExclusionPattern()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private data class ExclusionPattern(val pattern: String)