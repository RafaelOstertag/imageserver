package ch.guengel.imageserver.image

import ch.guengel.imageserver.directory.DirectoryWatcher
import ch.guengel.imageserver.directory.EntryType
import ch.guengel.imageserver.directory.WatchCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.asKotlinRandom

class ImageService(root: Path) {
    private val imageLister = ImageLister(root)
    private val imageEvents = Channel<ImageEvent>()
    private val imageEventCallback = ImageEventCallback(imageEvents, Image.imagePatternMatcher)
    private val directoryWatcher = DirectoryWatcher(root, imageEventCallback)
    private var allImages = ConcurrentSkipListSet<Path>()
    private val rng = ThreadLocalRandom.current().asKotlinRandom()

    init {
        GlobalScope.launch {
            readAll()
        }
        listenToEvents()
        directoryWatcher.start()
    }

    fun getRandomImage(width: Int, height: Int): Image {
        val image = allImages.random(rng)
        logger.info("Serving image {}", image)
        val originalImage = Image(image)
        return originalImage.resizeToMatch(width, height)
    }

    suspend fun readAll() {
        logger.info("Start updating image list")
        allImages.clear()
        for (path in imageLister.getImages()) {
            allImages.add(path)
        }

        logger.info("Done updating image list: {} image(s)", allImages.size)
    }

    private fun listenToEvents() {
        GlobalScope.launch {
            while (isActive) {
                val imageEvent = imageEvents.receive()
                when (imageEvent.pathType) {
                    EntryType.DIRECTORY -> handleDirectoryEvent(imageEvent.imagePath, imageEvent.type)
                    EntryType.FILE -> handleFileEvent(imageEvent.imagePath, imageEvent.type)
                }
            }
        }
    }

    private fun handleFileEvent(imagePath: Path, type: ImageEventType) {
        with(allImages) {
            when (type) {
                ImageEventType.DELETED -> {
                    remove(imagePath)
                    logger.debug("Removed {} from image list", imagePath)
                }
                ImageEventType.CREATED -> {
                    add(imagePath)
                    logger.debug("Added {} to image list", imagePath)
                }
            }
        }
    }

    private fun handleDirectoryEvent(imagePath: Path, type: ImageEventType) {
        if (type == ImageEventType.DELETED) {
            allImages.removeIf {
                it.startsWith(imagePath).also {
                    if (it) {
                        logger.debug("Remove {} from image list", imagePath)
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImageService::class.java)
    }
}

private class ImageEventCallback(private val channel: Channel<ImageEvent>, private val patternMatcher: Regex) :
    WatchCallback {
    override fun deleted(path: Path, type: EntryType) {
        if (type == EntryType.FILE && !patternMatcher.matches(path.toString())) {
            return
        }

        GlobalScope.launch {
            channel.send(ImageEvent(path, ImageEventType.DELETED, type))
        }
    }

    override fun created(path: Path, type: EntryType) {
        if (type == EntryType.DIRECTORY || !patternMatcher.matches(path.toString())) {
            // we don't care about new directories, only new images
            return
        }

        GlobalScope.launch {
            channel.send(ImageEvent(path, ImageEventType.CREATED, type))
        }
    }

    override fun modified(path: Path, type: EntryType) {
        // we don't care about modification
    }
}

private enum class ImageEventType {
    CREATED,
    DELETED
}

private data class ImageEvent(val imagePath: Path, val type: ImageEventType, val pathType: EntryType)