package ch.guengel.imageserver.rest

import assertk.assertThat
import assertk.assertions.isEqualTo
import ch.guengel.imageserver.image.Image
import ch.guengel.imageserver.image.ImageService
import io.mockk.*
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.regex.PatternSyntaxException
import javax.inject.Inject
import javax.ws.rs.core.MediaType

@QuarkusTest
internal class ImagesTest {
    @Inject
    private lateinit var imageService: ImageService

    private val image = Image(Path.of("src/test/resources/images/small.png"))


    @Test
    fun getImage() {
        every { imageService.getRandomImage(any(), any()) } returns image

        given().`when`().get("/images/720/820").then().statusCode(200)

        verify { imageService.getRandomImage(720, 820) }
    }

    @Test
    fun `invalid width`() {
        given().`when`().get("/images/99/820").then().statusCode(400)

        verify(exactly = 0) { imageService.getRandomImage(any(), any()) }
    }

    @Test
    fun `invalid height`() {
        given().`when`().get("/images/820/99").then().statusCode(400)

        verify(exactly = 0) { imageService.getRandomImage(any(), any()) }
    }

    @Test
    fun rereadImages() {
        coEvery { imageService.readAll() } just Runs

        given().`when`().put("/images/?update").then().statusCode(204)

        coVerify { imageService.readAll() }
    }

    @Test
    fun getExclusions() {
        every { imageService.getExclusionPattern() } returns "exclusion pattern"
        val response = given().`when`().get("/images/exclusions").then().statusCode(200)
                .extract().response().`as`(ExclusionPattern::class.java)
        assertThat(response.pattern).isEqualTo("exclusion pattern")
    }

    @Test
    fun updateExclusions() {
        every { imageService.setExclusionPattern(any()) } just Runs
        given().contentType(MediaType.APPLICATION_JSON).body("""{ "pattern": "exclusion" }""")
            .`when`().put("/images/exclusions")
            .then().statusCode(204)
    }

    @Test
    fun `validate size of exclusion pattern`() {
        given().contentType(MediaType.APPLICATION_JSON).body("""{ "pattern": "e" }""")
            .`when`().put("/images/exclusions")
            .then().statusCode(400)
    }

    @Test
    fun `handle invalid pattern`() {
        every { imageService.setExclusionPattern(any()) } throws PatternSyntaxException("test", "test", 0)
        given().contentType(MediaType.APPLICATION_JSON).body("""{ "pattern": "[" }""")
            .`when`().put("/images/exclusions")
            .then().statusCode(400)
    }

    @Test
    fun deleteExclusionPattern() {
        every { imageService.resetExclusionPattern() } just Runs
        given().`when`().delete("/images/exclusions")
            .then().statusCode(204)
        verify { imageService.resetExclusionPattern() }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            QuarkusMock.installMockForType(mockk(), ImageService::class.java)
        }
    }
}