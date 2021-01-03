package ch.guengel.imageserver.image

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class ImageServiceTest {

    @Test
    fun getRandomImage() {
        val imageService =
            ImageService(Path.of("src/test/resources/images"))
        runBlocking {
            delay(5000)
        }
        val image = imageService.getRandomImage(300, 400)


        assertThat(image.width).isEqualTo(300)
        assertThat(image.height).isEqualTo(400)
    }
}