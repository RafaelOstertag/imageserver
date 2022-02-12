package ch.guengel.imageserver.k8s

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.api.model.PodListBuilder
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.kubernetes.client.KubernetesTestServer
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject

@WithKubernetesTestServer
@QuarkusTest
internal class ManagementServiceIT {
    @Inject
    private lateinit var managementService: ManagementService

    @KubernetesTestServer
    private lateinit var kubernetesMockServer: KubernetesServer

    private var wireMockServer: WireMockServer? = null

    @BeforeEach
    fun before() {
        val pod1 = PodBuilder().withNewMetadata()
            .withName("imageserver-1")
            .withNamespace("test")
            .endMetadata()
            .withNewStatus()
            .withPodIP("127.0.0.1")
            .endStatus()
            .build()
        val pod2 = PodBuilder().withNewMetadata()
            .withName("imageserver-2")
            .withNamespace("test")
            .endMetadata()
            .withNewStatus()
            .withPodIP("127.0.0.1")
            .endStatus()
            .build()

        kubernetesMockServer.expect().get().withPath("/api/v1/namespaces/test/pods")
            .andReturn(
                200,
                PodListBuilder().withNewMetadata().withResourceVersion("1").endMetadata().withItems(pod1, pod2)
                    .build()
            )
            .always()
    }

    @AfterEach
    fun after() {
        wireMockServer?.stop()
    }

    @Test
    fun `reload all instances`() {
        wireMockServer = WireMockServer(WireMockConfiguration.options().port(32001)).also {
            it.givenThat(put(urlEqualTo("/images?update=")).willReturn(noContent()))
            it.start()
        }

        val reloadAllInstances = managementService.reloadAllInstances()
        reloadAllInstances.forEach {
            assertThat(it.errorMsg).isNull()
            assertThat(it.httpStatus).isEqualTo(204)
        }

        wireMockServer?.verify(2, putRequestedFor(urlEqualTo("/images?update=")))
    }

    @Test
    fun `reset all executions`() {
        wireMockServer = WireMockServer(WireMockConfiguration.options().port(32001)).also {
            it.givenThat(delete(urlEqualTo("/images/exclusions")).willReturn(noContent()))
            it.start()
        }

        val reloadAllInstances = managementService.resetAllExclusions()
        reloadAllInstances.forEach {
            assertThat(it.errorMsg).isNull()
            assertThat(it.httpStatus).isEqualTo(204)
        }

        wireMockServer?.verify(2, deleteRequestedFor(urlEqualTo("/images/exclusions")))
    }

    @Test
    fun `set all executions`() {
        wireMockServer = WireMockServer(WireMockConfiguration.options().port(32001)).also {
            it.givenThat(put(urlEqualTo("/images/exclusions")).willReturn(noContent()))
            it.start()
        }

        val reloadAllInstances = managementService.updateAllExclusions("test")
        reloadAllInstances.forEach {
            assertThat(it.errorMsg).isNull()
            assertThat(it.httpStatus).isEqualTo(204)
        }

        wireMockServer?.verify(
            2, putRequestedFor(urlEqualTo("/images/exclusions")).withRequestBody(
                matchingJsonPath("$.pattern", equalTo("test"))
            )
        )
    }

    @Test
    fun `handle connection errors`() {
        val reloadAllInstances = managementService.updateAllExclusions("test")
        reloadAllInstances.forEach {
            assertThat(it.errorMsg).isNotNull()
            assertThat(it.httpStatus).isEqualTo(-1)
        }
    }
}
