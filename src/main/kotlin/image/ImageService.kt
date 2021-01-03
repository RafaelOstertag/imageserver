package ch.guengel.imageserver.image

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

private const val defaultExclusionPattern = "^$"

class ImageService(private val root: Path) {
    private var allImages = ConcurrentSkipListSet<Path>()
    private val rng = Random(System.currentTimeMillis())
    private var excludeRegexRef = AtomicReference<Regex>(Regex(defaultExclusionPattern))

    init {
        GlobalScope.launch {
            readAll()
        }
    }

    fun getRandomImage(width: Int, height: Int): Image {
        val image = allImages.random(rng)
        logger.info("Serving image {}", image)
        val originalImage = Image(image)
        return originalImage.resizeToMatch(width, height)
    }

    fun setExclusionPattern(pattern: String) {
        val newRegex = Regex(pattern)
        val oldRegex = excludeRegexRef.getAndSet(newRegex)
        if (newRegex.toString() != oldRegex.toString()) {
            GlobalScope.launch {
                readAll()
            }
        }
    }

    fun resetExclusionPattern() {
        val newRegex = Regex(defaultExclusionPattern)
        val oldRegex = excludeRegexRef.getAndSet(newRegex)
        if (oldRegex.toString() != newRegex.toString()) {
            GlobalScope.launch {
                readAll()
            }
        }
    }

    fun getExclusionPattern(): String = excludeRegexRef.get().pattern

    suspend fun readAll() {
        logger.info("Start updating image list")
        val imageLister = ImageLister(root, excludeRegexRef.get())
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