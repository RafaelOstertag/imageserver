package ch.guengel.imageserver.image

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.io.File

internal class ImageTest {

    @Test
    fun getWidth() {
        val image = Image(File("src/test/resources/images/small.png"))
        assertThat(image.width).isEqualTo(300)
    }

    @Test
    fun getHeight() {
        val image = Image(File("src/test/resources/images/small.png"))
        assertThat(image.height).isEqualTo(600)
    }

    @Test
    fun resizeToMatch() {
        val image = Image(File("src/test/resources/images/small.png"))
        val resized = image.resizeToMatch(800, 900)
        assertThat(resized.width).isEqualTo(800)
        assertThat(resized.height).isEqualTo(900)
    }
}