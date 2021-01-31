package ch.guengel.imageserver.image

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Singleton
import kotlin.random.Random

private const val defaultExclusionPattern = "^$"

@Singleton
class ImageService(@ConfigProperty(name = "images.directory") private val root: Path) {
    private var allImages = ConcurrentSkipListSet<Path>()
    private val rng = Random(System.currentTimeMillis())
    private var excludeRegexRef = AtomicReference<Regex>(Regex(defaultExclusionPattern))

    init {
        runBlocking {
            readAll()
        }
    }

    fun getRandomImage(width: Int, height: Int): Image {
        val image = allImages.random(rng)
        logger.info("Serving ch.guengel.imageserver.image $image")
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
        logger.info("Start updating ch.guengel.imageserver.image list")
        val imageLister = ImageLister(root, excludeRegexRef.get())
        allImages.clear()
        for (path in imageLister.getImages()) {
            allImages.add(path)
        }

        logger.info(
            "Done updating ch.guengel.imageserver.image list: ${allImages.size} ch.guengel.imageserver.image(s)"
        )
    }
    companion object {
        private val logger = Logger.getLogger(ImageService::class.java)
    }
}