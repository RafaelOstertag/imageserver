package ch.guengel.imageserver.image

import ch.guengel.imageserver.directory.DirectoryLister
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Integer.max
import java.util.concurrent.Executors


class ImageLister(directory: String) {

    private val directoryLister =
        DirectoryLister(directory, ".*\\.(?:jpe?g|png|gif)$")

    private suspend fun readImageInformation() = withContext(fileReadPool) {
        directoryLister
            .getFiles()
            .map { file ->
                async {
                    try {
                        createImageInfo(file)
                    } catch (e: Exception) {
                        logger.error("Error loading file {}", file.canonicalPath, e)
                        ImageInfo(file, ImageSize.MEDIUM)
                    }
                }
            }.awaitAll()
    }

    private fun createImageInfo(imageFile: File): ImageInfo {
        logger.debug("Read file {}", imageFile.canonicalPath)
        val imageBuffer = Image(imageFile)
        val width = imageBuffer.width
        val height = imageBuffer.height

        val largestSide = max(width, height)

        return ImageInfo(imageFile, toImageSize(largestSide))
    }

    private fun toImageSize(largestSide: Int) = when {
        largestSide < 1024 -> ImageSize.SMALL
        largestSide < 1200 -> ImageSize.MEDIUM
        else -> ImageSize.LARGE
    }

    fun images() = runBlocking { readImageInformation() }

    companion object {
        private val logger = LoggerFactory.getLogger(ImageLister::class.java)
        private val fileReadPool = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    }
}