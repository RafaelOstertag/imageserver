package ch.guengel.imageserver.image

import kotlinx.coroutines.*
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.enterprise.context.ApplicationScoped
import kotlin.random.Random

private const val defaultExclusionPattern = "^$"

@ApplicationScoped
class ImageService(@ConfigProperty(name = "images.directory") private val root: Path) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var allImages = ConcurrentSkipListSet<Path>()
    private val rng = Random(System.currentTimeMillis())
    private var excludeRegexRef = AtomicReference<Regex>(Regex(defaultExclusionPattern))

    @PostConstruct
    internal fun postConstruct() {
        runBlocking {
            readAll()
        }
    }

    @PreDestroy
    internal fun preDestroy() {
        scope.cancel()
    }

    fun getRandomImage(width: Int, height: Int): Image {
        val imagePath = allImages.random(rng)
        logger.info("Serving image $imagePath")
        val originalImage = Image(imagePath)
        return originalImage.resizeToMatch(width, height)
    }

    fun setExclusionPattern(pattern: String) {
        val newRegex = Regex(pattern)
        val oldRegex = excludeRegexRef.getAndSet(newRegex)
        if (newRegex.toString() != oldRegex.toString()) {
            scope.launch {
                readAll()
            }
        }
    }

    fun resetExclusionPattern() {
        val newRegex = Regex(defaultExclusionPattern)
        val oldRegex = excludeRegexRef.getAndSet(newRegex)
        if (oldRegex.toString() != newRegex.toString()) {
            scope.launch {
                readAll()
            }
        }
    }

    fun getExclusionPattern(): String = excludeRegexRef.get().pattern

    suspend fun readAll() {
        logger.info("Start image list")
        ImageLister(root, excludeRegexRef.get()).use { imageLister ->
            allImages.clear()
            for (path in imageLister.getImages()) {
                allImages.add(path)
            }

            logger.info(
                "Done updating image list: ${allImages.size} image(s)"
            )
        }
    }

    companion object {
        private val logger = Logger.getLogger(ImageService::class.java)
    }
}
