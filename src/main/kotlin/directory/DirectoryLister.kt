package ch.guengel.imageserver.directory

import org.slf4j.LoggerFactory
import java.nio.file.Path

class DirectoryLister(directory: Path, private val patternMatcher: Regex) {
    private val directory = directory.toFile()

    fun getFiles() =
        directory
            .walk()
            .onFail { file, ioException ->
                logger.error(
                    "Error reading {}: {}",
                    file.name,
                    ioException.message
                )
            }
            .filter { file -> file.isFile && patternMatcher.matches(file.name) }
            .map { file -> file.toPath() }
            .toList()


    companion object {
        private val logger = LoggerFactory.getLogger(DirectoryLister::class.java)
    }
}