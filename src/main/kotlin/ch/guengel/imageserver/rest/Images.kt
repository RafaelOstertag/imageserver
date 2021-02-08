package ch.guengel.imageserver.rest

import ch.guengel.imageserver.image.ImageService
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Uni
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.RestQuery
import javax.inject.Inject
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.Size
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/images")
class Images(@Inject private val imageService: ImageService) {

    @GET
    @Path("/{width}/{height}")
    @Produces("image/jpeg")
    @Blocking
    fun getImage(@Valid @Min(100) width: Int, @Valid @Min(100) height: Int): Uni<ByteArray> = Uni
        .createFrom()
        .item(imageService.getRandomImage(width, height))
        .onItem()
        .transform { it.toByteArray() }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    fun rereadImages(@RestQuery update: String?): Uni<Response> {
        if (update == null) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).build())
        }
        GlobalScope.launch(Dispatchers.IO) {
            imageService.readAll()
        }
        return Uni.createFrom().item(Response.noContent().build())
    }

    @GET
    @Path("/exclusions")
    @Produces(MediaType.APPLICATION_JSON)
    fun getExclusions(): Uni<ExclusionPattern> {
        return Uni.createFrom().item(imageService.getExclusionPattern())
            .onItem().transform { ExclusionPattern(it) }
    }

    @PUT
    @Path("/exclusions")
    @Produces(MediaType.APPLICATION_JSON)
    fun updateExclusions(@Valid exclusionPattern: ExclusionPattern): Uni<Response> {
        return Uni.createFrom().item(exclusionPattern)
            .onItem().transform { imageService.setExclusionPattern(exclusionPattern.pattern) }
            .onItem().transform { Response.noContent() }
            .onItem().transform { it -> it.build() }
    }

    @DELETE
    @Path("/exclusions")
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteExclusionPattern(): Uni<Response> {
        imageService.resetExclusionPattern()
        return Uni.createFrom().item(Response.noContent().build())
    }
}

data class ExclusionPattern(@field:Size(min = 2) val pattern: String)