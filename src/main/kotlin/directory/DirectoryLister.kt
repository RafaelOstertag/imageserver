package ch.guengel.imageserver.directory

import org.slf4j.LoggerFactory
import java.io.File

class DirectoryLister(directory: String, pattern: String = ".*") {
    private val patternMatcher = Regex(pattern)
    private val directory = File(directory)

    fun getFiles() =
        directory.walk().onFail { file, ioException ->
            logger.error(
                "Error reading {}: {}",
                file.name,
                ioException.message
            )
        }
            .filter { file -> file.isFile && patternMatcher.matches(file.name) }
            .toList()


    companion object {
        private val logger = LoggerFactory.getLogger(DirectoryLister::class.java)
    }
}