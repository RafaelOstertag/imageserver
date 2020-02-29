package ch.guengel.imageserver.image

import ch.guengel.imageserver.directory.DirectoryLister
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors


class ImageLister(directory: String) {

    private val directoryLister =
        DirectoryLister(directory, Image.imagePattern)

    private suspend fun readImageInformation(imageChannel: Channel<ImageInfo>) = withContext(fileReadPool) {
        directoryLister
            .getFiles()
            .forEach { file ->
                try {
                    imageChannel.send(ImageInfo.fromFile(file))
                } catch (e: Exception) {
                    logger.error("Error loading file {}", file.canonicalPath, e)
                    imageChannel.send(ImageInfo(file, ImageSize.MEDIUM, ImageEvent.UPDATE))
                }
            }
        imageChannel.close()
    }

    fun images(): Channel<ImageInfo> {
        val imageChannel = Channel<ImageInfo>()


        GlobalScope.launch { readImageInformation(imageChannel) }
        return imageChannel
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ImageLister::class.java)
        private val fileReadPool = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    }
}