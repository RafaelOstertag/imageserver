package ch.guengel.imageserver.rest

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.util.regex.PatternSyntaxException
import javax.validation.ValidationException


internal class ExceptionMapperTest {
    private val exceptionMapper = ExceptionMapper()

    @Test
    fun mapValidationException() {
        val x = ValidationException("message")
        val response = exceptionMapper.mapValidationException(x)
        val errorResponse = response.entity as ErrorResponse
        assertThat(errorResponse.message).isEqualTo("message")
    }

    @Test
    fun mapPatternSyntaxException() {
        val x = PatternSyntaxException("desc", "regex", 1)
        val response = exceptionMapper.mapPatternSyntaxException(x)
        val errorResponse = response.entity as ErrorResponse
        assertThat(errorResponse.message).isEqualTo("desc near index 1\nregex\n ^")
    }
}