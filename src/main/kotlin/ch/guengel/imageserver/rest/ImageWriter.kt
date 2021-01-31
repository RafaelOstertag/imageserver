package ch.guengel.imageserver.rest

import ch.guengel.imageserver.image.Image
import java.io.OutputStream
import java.lang.reflect.Type
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.ext.MessageBodyWriter
import javax.ws.rs.ext.Provider

@Provider
class ImageWriter : MessageBodyWriter<Image> {
    override fun isWriteable(
        type: Class<*>,
        genericType: Type?,
        annotations: Array<out Annotation>?,
        mediaType: MediaType?
    ): Boolean = type == Image::class.java

    override fun writeTo(
        t: Image,
        type: Class<*>?,
        genericType: Type?,
        annotations: Array<out Annotation>?,
        mediaType: MediaType?,
        httpHeaders: MultivaluedMap<String, Any>?,
        entityStream: OutputStream
    ) {
        t.write(entityStream)
    }
}