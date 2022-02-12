package ch.guengel.imageserver.rest


import assertk.assertThat
import assertk.assertions.isEqualTo
import ch.guengel.imageserver.k8s.ManagementService
import ch.guengel.imageserver.k8s.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.common.mapper.TypeRef
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType

@QuarkusTest
internal class ManagementIT {
    @InjectMock
    private lateinit var managementService: ManagementService

    @Test
    fun reloadAll() {
        val expectedResponse = listOf(Result("ip", 42, "error"))
        every { managementService.reloadAllInstances() } returns expectedResponse
        val response = given().`when`().put("/management/images/reload").then().statusCode(200)
            .extract().response().`as`(object : TypeRef<List<Result>>() {})
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun deleteExclusionPattern() {
        val expectedResponse = listOf(Result("ip", 42, "error"))
        every { managementService.resetAllExclusions() } returns expectedResponse
        val response = given().`when`().delete("/management/exclusions").then().statusCode(200)
            .extract().response().`as`(object : TypeRef<List<Result>>() {})
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun updateExclusions() {
        val expectedResponse = listOf(Result("ip", 42, "error"))
        every { managementService.updateAllExclusions(any()) } returns expectedResponse

        val response = given().contentType(MediaType.APPLICATION_JSON).body("""{ "pattern": "exclusion" }""")
            .`when`().put("/management/exclusions")
            .then().statusCode(200)
            .extract().response().`as`(object : TypeRef<List<Result>>() {})
        assertThat(response).isEqualTo(expectedResponse)

        verify { managementService.updateAllExclusions("exclusion") }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            QuarkusMock.installMockForType(mockk(), ManagementService::class.java)
        }
    }
}
