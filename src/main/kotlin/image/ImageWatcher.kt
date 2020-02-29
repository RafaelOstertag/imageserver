package ch.guengel.imageserver.image

import ch.guengel.imageserver.directory.DirectoryWatcher
import ch.guengel.imageserver.directory.FileEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File


class ImageWatcher(directory: String) : AutoCloseable {
    private val imageRegex = Regex(Image.imagePattern)
    private val directoryWatcherChannel = Channel<FileEvent>()
    val imageChannel = Channel<ImageInfo>()
    private val imageWatcher = DirectoryWatcher(directory, directoryWatcherChannel)

    fun start() {
        imageWatcher.watch()
        GlobalScope.launch {
            for (fileEvent in directoryWatcherChannel) {
                if (!imageRegex.matches(fileEvent.filepath)) continue

                when (fileEvent.eventType.asImageEvent()) {
                    ImageEvent.DELETE -> imageChannel.send(
                        ImageInfo(
                            File(fileEvent.filepath),
                            ImageSize.SMALL,
                            ImageEvent.DELETE
                        )
                    )
                    ImageEvent.UPDATE -> imageChannel.send(ImageInfo.fromFile(File(fileEvent.filepath)))
                }
            }
        }
    }

    override fun close() {
        directoryWatcherChannel.close()
        imageChannel.close()
    }


}

private fun FileEvent.EventType.asImageEvent() = when (this) {
    FileEvent.EventType.CREATED, FileEvent.EventType.MODIFIED -> ImageEvent.UPDATE
    FileEvent.EventType.DELETED -> ImageEvent.DELETE
}