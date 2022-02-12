package ch.guengel.imageserver.k8s

import ch.guengel.imageserver.rest.ExclusionPattern
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.*
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.RuntimeType
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

data class Result(val ip: String, val httpStatus: Int, val errorMsg: String?)

@ApplicationScoped
class ManagementService(
    @ConfigProperty(name = "k8s.podname") private val podname: String,
    @ConfigProperty(name = "k8s.podport") private val podPort: String,
    @ConfigProperty(name = "k8s.http.client.timeout-seconds", defaultValue = "15") private val clientTimeout: Long,
    private val kubernetesClient: KubernetesClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @PreDestroy
    internal fun preDestroy() {
        scope.cancel()
    }

    private fun getAllPodIPs(): List<String> {
        return kubernetesClient.pods().list().items
            .filter { pod -> pod.metadata.name.startsWith(podname) }
            .map { pod -> pod.status.podIP }
            .apply {
                logger.info("Identified following IP(s): ${this.joinToString()}")
            }
    }

    private fun runWithClient(block: (Client, String) -> Result): List<Result> {
        val allPodIPs = getAllPodIPs()
        val client = ClientBuilder.newBuilder().withConfig(ConfigurationImpl(RuntimeType.CLIENT))
            .connectTimeout(clientTimeout, TimeUnit.SECONDS)
            .build()
        try {
            val jobs = allPodIPs.map { ip ->
                scope.async {
                    try {
                        block(client, ip)
                    } catch (ex: RuntimeException) {
                        Result(ip, -1, ex.message)
                    }
                }
            }
            logger.info("Waiting for all jobs to complete")
            val results = runBlocking {
                jobs.map { result -> result.await() }
            }

            logger.info("Done running jobs")
            return results
        } finally {
            client.close()
        }
    }

    fun reloadAllInstances() =
        runWithClient { client, ip ->
            logger.info("Notifying $ip to reload")
            val response = client
                .target("http://$ip:$podPort/images?update=")
                .request()
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE))
            val result = Result(ip, response.status, null)
            response.close()
            result
        }

    fun resetAllExclusions() =
        runWithClient { client, ip ->
            logger.info("Notifying $ip to reset exclusions")
            val response = client
                .target("http://$ip:$podPort/images/exclusions")
                .request()
                .delete()
            val result = Result(ip, response.status, null)
            response.close()
            result
        }

    fun updateAllExclusions(pattern: String): List<Result> = pattern.let {
        val exclusionPattern = ExclusionPattern(it)
        runWithClient { client, ip ->
            logger.info("Set exclusion pattern on $ip")
            val response = client
                .target("http://$ip:$podPort/images/exclusions")
                .request()
                .put(Entity.entity(exclusionPattern, MediaType.APPLICATION_JSON_TYPE))
            val result = Result(ip, response.status, null)
            response.close()
            result
        }
    }

    companion object {
        private val logger = Logger.getLogger(ManagementService::class.java)
    }
}
