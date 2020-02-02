package ch.guengel.imageserver.image

import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random


class ImageService(private val imageLister: ImageLister) {
    private var imageList = AtomicReference(imageLister.images())
    private var largeImageList =
        AtomicReference(imageList.get().filter { imageInfo -> imageInfo.size == ImageSize.LARGE || imageInfo.size == ImageSize.MEDIUM })
    private val timer = Timer().apply {
        schedule(UpdateImageList(imageLister, imageList, largeImageList), 5 * 60 * 1_000, 5 * 60 * 1_000)
    }
    private val rng = Random(System.currentTimeMillis())

    fun getRandomImage(width: Int, height: Int): Image {
        val imageInfo = imageList.get().random(rng)
        val originalImage = Image(imageInfo.path)
        return originalImage.resizeToMatch(width, height)
    }

    fun getLargeRandomImage(width: Int, height: Int): Image {
        val imageInfo = largeImageList.get().random(rng)
        val originalImage = Image(imageInfo.path)
        return originalImage.resizeToMatch(width, height)
    }

    private class UpdateImageList(
        private val imageLister: ImageLister,
        private val imageList: AtomicReference<List<ImageInfo>>,
        private val largeImageList: AtomicReference<List<ImageInfo>>
    ) : TimerTask() {
        override fun run() {
            logger.info("Start updating image list")
            val newImageList = imageLister.images()
            imageList.set(newImageList)

            val newLargeImageList =
                newImageList.filter { imageInfo -> imageInfo.size == ImageSize.LARGE || imageInfo.size == ImageSize.MEDIUM }
            largeImageList.set(newLargeImageList)

            logger.info("Done updating image list: {} image(s)", newImageList.size)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImageService::class.java)
    }
}