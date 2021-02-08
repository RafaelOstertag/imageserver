package ch.guengel.imageserver.image

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import javax.imageio.IIOException

internal class ImageTest {
    @Test
    fun `load image`() {
        val image = Image(Path.of("src/test/resources/images/small.png"))
        assertThat(image.width).isEqualTo(300)
        assertThat(image.height).isEqualTo(600)
    }

    @Test
    fun `resize image`() {
        val image = Image(Path.of("src/test/resources/images/small.png"))

        val resizedImage1 = image.resizeToMatch(100, 200)
        assertThat(resizedImage1.width).isEqualTo(100)
        assertThat(resizedImage1.height).isEqualTo(200)

        val resizedImage2 = image.resizeToMatch(200, 200)
        assertThat(resizedImage2.width).isEqualTo(200)
        assertThat(resizedImage2.height).isEqualTo(200)

        val resizedImage3 = image.resizeToMatch(300, 200)
        assertThat(resizedImage3.width).isEqualTo(300)
        assertThat(resizedImage3.height).isEqualTo(200)

        // Initial image must not be touched
        assertThat(image.width).isEqualTo(300)
        assertThat(image.height).isEqualTo(600)
    }

    @Test
    fun `load non existing image`() {
        assertThrows<IIOException> {
            Image(Path.of("/does/not/exist/image.png"))
        }
    }

    @Test
    fun `image byte array`() {
        val image = Image(Path.of("src/test/resources/images/small.png"))
        val byteArray = image.toByteArray()

        assertImageByteArray(byteArray)
    }

    private fun assertImageByteArray(byteArray: ByteArray) {
        assertThat(byteArray).isNotEmpty()
        assertThat(byteArray[0]).isEqualTo(-1)
        assertThat(byteArray[1]).isEqualTo(-40)
    }

    @Test
    fun `image writer`() {
        val image = Image(Path.of("src/test/resources/images/small.png"))
        val byteArrayOutputStream = ByteArrayOutputStream()
        image.write(byteArrayOutputStream)

        assertImageByteArray(byteArrayOutputStream.toByteArray())
    }
}