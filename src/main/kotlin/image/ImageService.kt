package ch.guengel.imageserver.image

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.asKotlinRandom

private fun largeImagePredicate(imageInfo: ImageInfo) = imageInfo.size == ImageSize.LARGE
private const val updateInterval = 15 * 60 * 1_000L

class ImageService(private val imageLister: ImageLister, private val imageWatcher: ImageWatcher) {
    private var allImages = ConcurrentSkipListSet<String>()
    private val largeImages = ConcurrentSkipListSet<String>()
    private val rng = ThreadLocalRandom.current().asKotlinRandom()

    init {
        readAll()
        watchDirectory()
    }

    fun getRandomImage(width: Int, height: Int): Image = getRandomImage(allImages, width, height)

    fun getLargeRandomImage(width: Int, height: Int): Image = getRandomImage(largeImages, width, height)

    private fun getRandomImage(imageList: ConcurrentSkipListSet<String>, width: Int, height: Int): Image {
        val image = imageList.random(rng)
        logger.info("Serving image {}", image)
        val originalImage = Image(File(image))
        return originalImage.resizeToMatch(width, height)
    }

    fun readAll() {
        GlobalScope.launch {
            logger.info("Start updating image list")
            allImages.clear()
            largeImages.clear()
            for (image in imageLister.images()) {
                addImageToLists(image)

            }
            logger.info("Done updating image list: {} image(s)", allImages.size)

        }
    }

    private fun addImageToLists(image: ImageInfo) {
        val filePath = image.path.canonicalPath
        allImages.add(filePath)
        if (largeImagePredicate(image)) {
            largeImages.add(filePath)
        }
    }

    private fun watchDirectory() {
        GlobalScope.launch {
            imageWatcher.start()
            for (image in imageWatcher.imageChannel) {
                when (image.event) {
                    ImageEvent.UPDATE -> {
                        logger.info("Update image {}", image.path.canonicalPath)
                        addImageToLists(image)
                    }
                    ImageEvent.DELETE -> {
                        logger.info("Remove image {}", image.path.canonicalPath)
                        removeImageFromLists(image)
                    }
                }
            }
        }
    }

    private fun removeImageFromLists(image: ImageInfo) {
        val filePath = image.path.canonicalPath
        allImages.remove(filePath)
        largeImages.remove(filePath)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImageService::class.java)
    }
}