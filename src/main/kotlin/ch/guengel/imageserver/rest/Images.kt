package ch.guengel.imageserver.rest

import ch.guengel.imageserver.image.Image
import ch.guengel.imageserver.image.ImageService
import io.smallrye.mutiny.Uni
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.RestQuery
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/images")
class Images(@Inject private val imageService: ImageService) {

    @GET
    @Path("/{width}/{height}")
    @Produces("image/jpeg")
    fun getImage(width: Int, height: Int): Uni<Image> {
        val randomImage = imageService.getRandomImage(width, height)
        return Uni.createFrom().item(randomImage)
    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    fun rereadImages(@RestQuery update: String?): Uni<Response> {
        if (update == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).build())
        }
        GlobalScope.launch {
            imageService.readAll()
        }
        return Uni.createFrom().item(Response.noContent().build())
    }

    @PUT
    @Path("/exclusion")
    @Produces(MediaType.APPLICATION_JSON)
    fun updateExclusions(exclusionPattern: ExclusionPattern): Uni<Response> {
        return Uni.createFrom().item(exclusionPattern)
            .onItem().transform { imageService.setExclusionPattern(exclusionPattern.pattern) }
            .onItem().transform { Response.noContent() }
            .onFailure().recoverWithItem(Response.status(Response.Status.BAD_REQUEST))
            .onItem().transform { it -> it.build() }
    }

    @DELETE
    @Path("/exclusion")
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteExclusionPattern(): Uni<Response> {
        imageService.resetExclusionPattern()
        return Uni.createFrom().item(Response.noContent().build())
    }
}

data class ExclusionPattern(val pattern: String)