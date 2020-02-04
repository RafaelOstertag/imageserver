package ch.guengel.imageserver.image

import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

private fun largeImagePredicate(imageInfo: ImageInfo) = imageInfo.size == ImageSize.LARGE
private const val updateInterval = 5 * 60 * 1_000L

class ImageService(private val imageLister: ImageLister) {
    private var imageList = AtomicReference(imageLister.images())
    private var largeImageList =
        AtomicReference(imageList.get().filter(::largeImagePredicate))
    private val timer = Timer().apply {
        schedule(ImageListUpdater(imageLister, imageList, largeImageList), updateInterval, updateInterval)
    }
    private val rng = Random(System.currentTimeMillis())

    fun getRandomImage(width: Int, height: Int): Image = getRandomImage(imageList, width, height)

    fun getLargeRandomImage(width: Int, height: Int): Image = getRandomImage(largeImageList, width, height)

    private fun getRandomImage(imageList: AtomicReference<List<ImageInfo>>, width: Int, height: Int): Image {
        val imageInfo = imageList.get().random(rng)
        val originalImage = Image(imageInfo.path)
        return originalImage.resizeToMatch(width, height)
    }

    private class ImageListUpdater(
        private val imageLister: ImageLister,
        private val imageList: AtomicReference<List<ImageInfo>>,
        private val largeImageList: AtomicReference<List<ImageInfo>>
    ) : TimerTask() {
        override fun run() {
            logger.info("Start updating image list")
            val newImageList = imageLister.images()
            imageList.set(newImageList)

            val newLargeImageList =
                newImageList.filter(::largeImagePredicate)
            largeImageList.set(newLargeImageList)

            logger.info("Done updating image list: {} image(s)", newImageList.size)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImageService::class.java)
    }
}