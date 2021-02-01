package ch.guengel.imageserver.k8s

import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.*
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
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
    }

    fun reloadAllInstances() {
        val allPodIPs = getAllPodIPs()
        logger.info("Identified following IP(s): ${allPodIPs.joinToString()}")
        val client = ClientBuilder.newClient()

        val jobs = allPodIPs.map { ip ->
            GlobalScope.launch(Dispatchers.IO) {
                logger.info("Notifying $ip to reload")
                client
                    .target("http://$ip:8080/images?update=")
                    .request()
                    .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE))
                    .close()
            }
        }
        logger.info("Waiting for all notifications to complete")
        runBlocking { jobs.joinAll() }

        client.close()
        logger.info("Done notifying instances to reload images")
    }

    companion object {
        private val logger = Logger.getLogger(ManagementService::class.java)
    }
}