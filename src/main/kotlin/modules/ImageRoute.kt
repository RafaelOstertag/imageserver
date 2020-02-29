package ch.guengel.imageserver.modules

import ch.guengel.imageserver.image.Image
import ch.guengel.imageserver.image.ImageLister
import ch.guengel.imageserver.image.ImageService
import ch.guengel.imageserver.image.ImageWatcher
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondOutputStream
import io.ktor.routing.get
import io.ktor.routing.routing

fun Application.imageRoute() {
    var imageDirectory = environment.config.property("images.directory").getString()
    log.info("Read images from '{}'", imageDirectory)

    val imageService = ImageService(ImageLister(imageDirectory), ImageWatcher(imageDirectory))
    routing {
        get("/images/{width}/{height}") {
            val width = call.parameters["width"]?.toInt() ?: throw IllegalArgumentException("Missing image width")
            val height = call.parameters["height"]?.toInt() ?: throw IllegalArgumentException("Missing image height")
            val imageSize = call.parameters["size"] ?: ""

            val image: Image
            if (imageSize == "large") {
                image = imageService.getLargeRandomImage(width, height)
            } else {
                image = imageService.getRandomImage(width, height)
            }
            call.respondOutputStream(ContentType.Image.PNG, HttpStatusCode.OK) {
                image.writePNG(this)
            }
        }
    }
}