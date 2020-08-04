package ch.guengel.imageserver.directory

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class DirectoryLister(private val directory: Path, private val patternMatcher: Regex) {
    private val directoryAsFile = directory.toFile()

    fun getFiles(): Channel<Path> {
        val channel = Channel<Path>()
        GlobalScope.launch(Dispatchers.IO) {
            Files.walkFileTree(directory, FileVisitor(channel, patternMatcher))
            channel.close()
        }
        return channel
    }


    private class FileVisitor(private val channel: Channel<Path>, private val patternMatcher: Regex) :
        SimpleFileVisitor<Path>() {

        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
            file?.takeIf {
                patternMatcher.matches(it.toString())
            }?.let {
                runBlocking {
                    try {
                        channel.send(it)
                    } catch (e: CancellationException) {
                        logger.debug("Sending file to channel has been cancelled")
                    }
                }
            }

            return super.visitFile(file, attrs)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DirectoryLister::class.java)
    }
}