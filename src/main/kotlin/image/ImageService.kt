package ch.guengel.imageserver.image

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

private const val defaultExclusionPattern = "^$"
class ImageService(root: Path) {
    private val imageLister = ImageLister(root)
    private var allImages = ConcurrentSkipListSet<Path>()
    private val rng = Random(System.currentTimeMillis())
    private var excludeRegexRef = AtomicReference<Regex>(Regex(defaultExclusionPattern))

    init {
        GlobalScope.launch {
            readAll()
        }
    }

    fun getRandomImage(width: Int, height: Int): Image {
        val image = randomImagePath()
        logger.info("Serving image {}", image)
        val originalImage = Image(image)
        return originalImage.resizeToMatch(width, height)
    }

    fun setExclusionPattern(pattern: String) {
        excludeRegexRef.set(Regex(pattern))
    }

    fun resetExclusionPattern() {
        excludeRegexRef.set(Regex(defaultExclusionPattern))
    }

    fun getExclusionPattern(): String = excludeRegexRef.get().pattern

    private fun randomImagePath(): Path {
        var image = allImages.random(rng)
        val excludeRegex = excludeRegexRef.get()
        // That's the lazy version. It's ok for my use case.
        while (excludeRegex.containsMatchIn(image.toString())) {
            image = allImages.random(rng)
        }
        return image
    }

    suspend fun readAll() {
        logger.info("Start updating image list")
        allImages.clear()
        for (path in imageLister.getImages()) {
            allImages.add(path)
        }

        logger.info("Done updating image list: {} image(s)", allImages.size)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(ImageService::class.java)
    }
}