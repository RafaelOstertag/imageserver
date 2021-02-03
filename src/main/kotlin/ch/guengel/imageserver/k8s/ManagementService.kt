package ch.guengel.imageserver.k8s

import ch.guengel.imageserver.rest.ExclusionPattern
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.*
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

@Singleton
class ManagementService(
    @ConfigProperty(name = "k8s.podname") private val podname: String,
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

    private fun runWithClient(block: (Client, String) -> Unit) {
        val allPodIPs = getAllPodIPs()
        val client = ClientBuilder.newClient()
        val jobs = allPodIPs.map { ip ->
            GlobalScope.launch(Dispatchers.IO) {
                block(client, ip)
            }
        }
        logger.info("Waiting for all jobs to complete")
        runBlocking { jobs.joinAll() }

        client.close()
        logger.info("Done running jobs")
    }

    fun reloadAllInstances() =
        runWithClient { client, ip ->
            logger.info("Notifying $ip to reload")
            client
                .target("http://$ip:8080/images?update=")
                .request()
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE))
                .close()
        }

    fun resetAllExclusions() {
        runWithClient { client, ip ->
            logger.info("Notifying $ip to reset exclusions")
            client
                .target("http://$ip:8080/images/exclusions")
                .request()
                .delete()
                .close()
        }
    }

    fun updateAllExclusions(pattern: String) {
        val exclusionPattern = ExclusionPattern(pattern)
        runWithClient { client, ip ->
            logger.info("Set exclusion pattern on $ip")
            client
                .target("http://$ip:8080/images/exclusions")
                .request()
                .put(Entity.entity(exclusionPattern, MediaType.APPLICATION_JSON_TYPE))
                .close()
        }
    }

    companion object {
        private val logger = Logger.getLogger(ManagementService::class.java)
    }
}