package ch.guengel.imageserver.directory

import org.slf4j.LoggerFactory
import java.nio.file.Path

class Callback : WatchCallback {
    override fun deleted(path: Path, type: EntryType) {
        logger.info("Deleted {}: {}", path, type)
    }

    override fun created(path: Path, type: EntryType) {
        logger.info("Created {}: {}", path, type)
    }

    override fun modified(path: Path, type: EntryType) {
        logger.info("Modified {}: {}", path, type)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Callback::class.java)
    }
}

fun main(args: Array<String>) {
    DirectoryWatcher(Path.of("/home/rafi/test"), Callback()).use {
        it.start()
        Thread.sleep(60_000)
    }
}