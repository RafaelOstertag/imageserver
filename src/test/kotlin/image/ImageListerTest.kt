package ch.guengel.imageserver.image

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class ImageListerTest {
    @Test
    fun randomImage() {
        val imageLister = ImageLister(Path.of("src/test/resources/images"))
        val images = runBlocking {
            val fileList = mutableListOf<Path>()
            for (image in imageLister.getImages()) {
                fileList.add(image)
            }
            fileList
        }

        assertThat(images).hasSize(3)

        assertThat(images.find { image -> image.fileName.toString() == "large.jpeg" }).isNotNull()
        assertThat(images.find { image -> image.fileName.toString() == "medium.jpg" }).isNotNull()
        assertThat(images.find { image -> image.fileName.toString() == "small.png" }).isNotNull()
    }
}