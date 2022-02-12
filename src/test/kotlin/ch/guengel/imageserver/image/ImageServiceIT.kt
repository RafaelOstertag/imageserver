package ch.guengel.imageserver.image

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.quarkus.test.junit.QuarkusTest
import org.junit.After
import org.junit.jupiter.api.Test
import javax.inject.Inject

@QuarkusTest
internal class ImageServiceIT {
    @Inject
    internal lateinit var imageService: ImageService

    @After
    fun after() {
        imageService.resetExclusionPattern()
    }

    @Test
    fun getRandomImage() {
        val image = imageService.getRandomImage(300, 400)

        assertThat(image.width).isEqualTo(300)
        assertThat(image.height).isEqualTo(400)
    }

    @Test
    fun `exclusion pattern`() {
        assertThat(imageService.getExclusionPattern()).isEqualTo("^$")
        imageService.setExclusionPattern(".*any.*")

        assertThat(imageService.getExclusionPattern()).isEqualTo(".*any.*")

        imageService.resetExclusionPattern()
        assertThat(imageService.getExclusionPattern()).isEqualTo("^$")
    }
}
