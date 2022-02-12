package ch.guengel.imageserver.image

import ch.guengel.imageserver.directory.DirectoryLister
import kotlinx.coroutines.channels.Channel
import java.nio.file.Path

class ImageLister(directory: Path, excludePattern: Regex) : AutoCloseable {
    private val directoryLister =
        DirectoryLister(directory, Image.imagePatternMatcher, excludePattern)

    fun getImages(): Channel<Path> = directoryLister.getFiles()

    override fun close() {
        directoryLister.close()
    }
}
