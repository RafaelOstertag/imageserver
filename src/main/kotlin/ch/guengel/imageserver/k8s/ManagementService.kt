package ch.guengel.imageserver.k8s

import ch.guengel.imageserver.rest.ExclusionPattern
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

data class Result(val ip: String, val httpStatus: Int, val errorMsg: String?)

@Singleton
class ManagementService(
        @ConfigProperty(name = "k8s.podname") private val podname: String,
        @ConfigProperty(name = "k8s.podport") private val podPort: String,
        @Inject private val kubernetesClient: KubernetesClient
) {

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
        val client = ClientBuilder.newBuilder().connectTimeout(15, TimeUnit.SECONDS).build()
        val jobs = allPodIPs.map { ip ->
            GlobalScope.async(Dispatchers.IO) {
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
        client.close()
        logger.info("Done running jobs")
        return results
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