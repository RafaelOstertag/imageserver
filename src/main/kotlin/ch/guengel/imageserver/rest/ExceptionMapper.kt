package ch.guengel.imageserver.rest

import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import java.util.regex.PatternSyntaxException
import javax.validation.ValidationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

data class ErrorResponse(val message: String)

class ExceptionMapper {
    @ServerExceptionMapper
    fun mapValidationException(x: ValidationException): Response {
        return x.toResponse()
    }

    @ServerExceptionMapper
    fun mapPatternSyntaxException(x: PatternSyntaxException): Response {
        return x.toResponse()
    }

    private fun Exception.toResponse(): Response {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(ErrorResponse(this.message ?: "no message"))
            .build()
    }
}