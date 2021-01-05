package ch.guengel.imageserver.modules

import ch.guengel.imageserver.image.ImageService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.nio.file.Path
import java.util.regex.PatternSyntaxException

private const val imagesEndpoint: String = "/images"
private const val exclusionsEndpoint: String = "${imagesEndpoint}/exclusions"

fun Application.imageRoute() {
    val imageDirectory = environment.config.property("images.directory").getString()
    log.info("Read images from '{}'", imageDirectory)

    val imageService = ImageService(Path.of(imageDirectory))
    routing {
        get("/images/{width}/{height}") {
            val width = call.parameters["width"]?.toInt()
                ?: throw IllegalArgumentException("Missing image width")
            val height =
                call.parameters["height"]?.toInt()
                    ?: throw IllegalArgumentException("Missing image height")

            call.respondOutputStream(ContentType.Image.JPEG, HttpStatusCode.OK) {
                val image = imageService.getRandomImage(width, height)
                image.write(this)
            }
        }

        put(imagesEndpoint) {
            call.parameters["update"] ?: throw java.lang.IllegalArgumentException("missing update query")
            imageService.readAll()
            call.respond(HttpStatusCode.NoContent)
        }

        put(exclusionsEndpoint) {
            val exclusionPattern = call.receive<ExclusionPattern>()
            try {
                imageService.setExclusionPattern(exclusionPattern.pattern)
            } catch (e: PatternSyntaxException) {
                throw IllegalArgumentException("Pattern is invalid")
            }
            call.respond(HttpStatusCode.NoContent)
        }

        get(exclusionsEndpoint) {
            call.respond(ExclusionPattern(imageService.getExclusionPattern()))
        }

        delete(exclusionsEndpoint) {
            imageService.resetExclusionPattern()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private data class ExclusionPattern(val pattern: String)