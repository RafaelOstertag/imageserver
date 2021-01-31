package ch.guengel.imageserver.rest

import java.io.OutputStream
import java.lang.reflect.Type
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.ext.MessageBodyWriter
import javax.ws.rs.ext.Provider

@Provider
class ImageWriter : MessageBodyWriter<ByteArray> {
    override fun isWriteable(
        type: Class<*>,
        genericType: Type?,
        annotations: Array<out Annotation>?,
        mediaType: MediaType?
    ): Boolean = type == ByteArray::class.java

    override fun writeTo(
        t: ByteArray,
        type: Class<*>?,
        genericType: Type?,
        annotations: Array<out Annotation>?,
        mediaType: MediaType?,
        httpHeaders: MultivaluedMap<String, Any>?,
        entityStream: OutputStream
    ) = entityStream.write(t)
}