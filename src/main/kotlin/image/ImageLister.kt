package ch.guengel.imageserver.image

import ch.guengel.imageserver.directory.DirectoryLister
import kotlinx.coroutines.channels.Channel
import java.nio.file.Path


class ImageLister(directory: Path) {
    private val directoryLister =
        DirectoryLister(directory, Image.imagePatternMatcher)

    fun getImages(): Channel<Path> = directoryLister.getFiles()
}