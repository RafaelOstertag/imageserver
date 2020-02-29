package image

import assertk.assertThat
import assertk.assertions.isEqualTo
import ch.guengel.imageserver.image.ImageLister
import ch.guengel.imageserver.image.ImageService
import ch.guengel.imageserver.image.ImageWatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class ImageServiceTest {

    @Test
    fun getRandomImage() {
        val imageService =
            ImageService(ImageLister("src/test/resources/images"), ImageWatcher("src/test/resources/images"))
        runBlocking {
            delay(5000)
        }
        val image = imageService.getRandomImage(300, 400)


        assertThat(image.width).isEqualTo(300)
        assertThat(image.height).isEqualTo(400)
    }
}