package ch.guengel.imageserver.image

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class ImageListerTest {
    @Test
    fun randomImage() {
        val imageLister = ImageLister("src/test/resources/images")
        val images = mutableListOf<ImageInfo>()
        runBlocking {
            imageLister.images().consumeAsFlow().toList(images)
        }

        assertThat(images).hasSize(3)

        assertThat(images.find { imageInfo -> imageInfo.size == ImageSize.SMALL }).isNotNull()
        assertThat(images.find { imageInfo -> imageInfo.size == ImageSize.MEDIUM }).isNotNull()
        assertThat(images.find { imageInfo -> imageInfo.size == ImageSize.LARGE }).isNotNull()
    }
}