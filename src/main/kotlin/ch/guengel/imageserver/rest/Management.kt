package ch.guengel.imageserver.rest

import ch.guengel.imageserver.k8s.ManagementService
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Uni
import javax.inject.Inject
import javax.ws.rs.DELETE
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/management")
class Management(@Inject private val managementService: ManagementService) {

    @PUT
    @Path("/images/reload")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun reloadAll(): Uni<Response> {
        managementService.reloadAllInstances()
        return Uni.createFrom().item(Response.noContent().build())
    }

    @DELETE
    @Path("/exclusions")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun deleteExclusionPatter(): Uni<Response> {
        managementService.resetAllExclusions()
        return Uni.createFrom().item(Response.noContent().build())
    }

    @PUT
    @Path("/exclusions")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun updateExclusions(exclusionPattern: ExclusionPattern): Uni<Response> {
        managementService.updateAllExclusions(exclusionPattern.pattern)
        return Uni.createFrom().item(Response.noContent().build())
    }
}