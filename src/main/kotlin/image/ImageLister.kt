package ch.guengel.imageserver.image

import ch.guengel.imageserver.directory.DirectoryLister
import java.nio.file.Path


class ImageLister(directory: Path) {
    private val directoryLister =
        DirectoryLister(directory, Image.imagePatternMatcher)

    fun images(): List<Path> = directoryLister.getFiles()
}