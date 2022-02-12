package ch.guengel.imageserver.rest

import ch.guengel.imageserver.k8s.ManagementService
import ch.guengel.imageserver.k8s.Result
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Multi
import javax.ws.rs.DELETE
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/management")
class Management(private val managementService: ManagementService) {

    @PUT
    @Path("/images/reload")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun reloadAll(): Multi<Result> = Multi.createFrom().iterable(managementService.reloadAllInstances())

    @DELETE
    @Path("/exclusions")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun deleteExclusionPattern(): Multi<Result> = Multi.createFrom().iterable(managementService.resetAllExclusions())

    @PUT
    @Path("/exclusions")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun updateExclusions(exclusionPattern: ExclusionPattern): Multi<Result> = Multi
        .createFrom()
        .iterable(managementService.updateAllExclusions(exclusionPattern.pattern))
}
