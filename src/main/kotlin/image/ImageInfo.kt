package ch.guengel.imageserver.image

import java.io.File

enum class ImageSize {
    SMALL, MEDIUM, LARGE
}

data class ImageInfo(val path: File, val size: ImageSize)