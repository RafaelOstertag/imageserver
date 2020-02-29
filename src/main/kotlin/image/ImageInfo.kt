package ch.guengel.imageserver.image

import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.max

private val logger = LoggerFactory.getLogger("ch.guengel.imageserver.image.ImageInfoKt")

enum class ImageSize {
    SMALL, MEDIUM, LARGE
}

enum class ImageEvent {
    UPDATE,
    DELETE
}

data class ImageInfo(val path: File, val size: ImageSize, val event: ImageEvent) {
    companion object {
        fun fromFile(imageFile: File): ImageInfo {
            logger.debug("Read file {}", imageFile.canonicalPath)
            val imageBuffer = Image(imageFile)
            val width = imageBuffer.width
            val height = imageBuffer.height

            val largestSide = max(width, height)

            return ImageInfo(imageFile, toImageSize(largestSide), ImageEvent.UPDATE)
        }
    }
}


private fun toImageSize(largestSide: Int) = when {
    largestSide < 1024 -> ImageSize.SMALL
    largestSide < 1200 -> ImageSize.MEDIUM
    else -> ImageSize.LARGE
}