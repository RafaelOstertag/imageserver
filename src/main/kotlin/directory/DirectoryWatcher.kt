package ch.guengel.imageserver.directory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import java.util.concurrent.Executors

private val directoryWatcherDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val watchServiceDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
private val logger = LoggerFactory.getLogger(DirectoryWatcher::class.java)

class DirectoryWatcher(directory: String, private val fileEvents: Channel<FileEvent>) {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val subDirectoryWatchers = mutableMapOf<String, DirectoryWatcher>()
    private var watching = false
    val directoryToWatch = Path.of(directory).also {
        it.register(watchService, arrayOf(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY))
    }

    fun watch() {
        if (watching) return


        logger.info("Start watching directory ${directoryToWatch.toAbsolutePath()}")
        startSubDirectoryWatchers()
        CoroutineScope(directoryWatcherDispatcher).launch {
            watchService.use {
                startWork()
                logger.info("Stop watching directory ${directoryToWatch.toAbsolutePath()}")
                watching = false
            }
        }

        watching = true
    }

    private fun startSubDirectoryWatchers() {
        val filepath = directoryToWatch.toAbsolutePath().toFile().canonicalFile
        val canonicalFilePath = filepath.canonicalPath
        filepath.walk(FileWalkDirection.TOP_DOWN).onEnter {
            it.parentFile.canonicalPath == canonicalFilePath || it.canonicalPath == canonicalFilePath
        }.iterator().forEach {
            if (!it.isDirectory || canonicalFilePath == it.canonicalPath) return@forEach

            val subDirectoryWatcher = DirectoryWatcher(it.canonicalPath, fileEvents)
            subDirectoryWatcher.watch()
            subDirectoryWatchers[it.name] = subDirectoryWatcher
        }
    }

    private suspend fun startWork() {
        while (true) {
            try {
                work()
                return
            } catch (e: Exception) {
                logger.warn("Error while watching directory ${directoryToWatch.toFile().canonicalPath}. Continue", e)
            }
        }
    }

    private suspend fun work() {
        while (true) {
            val key = CoroutineScope(watchServiceDispatcher).async {
                logger.debug("Wait for WatchService")
                val key = watchService.take()
                logger.debug("WatchService returned")
                key
            }.await()

            handleEvents(key.pollEvents())
            if (!key.reset()) {
                logger.info("Directory has ${directoryToWatch} been deleted")
                return
            }
        }
    }

    private suspend fun handleEvents(pollEvents: List<WatchEvent<*>>) {
        pollEvents
            .filter {
                val overflow = it.kind() == OVERFLOW
                if (overflow) logger.debug("Overflow detected")
                !overflow
            }
            .forEach {
                val kind = it.kind()

                val event = it as WatchEvent<Path>
                val context = event.context()
                // This is required to make #isDirectory() work
                val file = directoryToWatch.resolve(context).toAbsolutePath().toFile()

                if (isOrWasDirectory(file)) {
                    handleDirectoryEvent(file, kind)
                } else {
                    handleFileEvent(file, kind)
                }
            }
    }

    private fun isOrWasDirectory(file: File): Boolean = file.isDirectory || subDirectoryWatchers.containsKey(file.name)

    private suspend fun handleFileEvent(file: File, kind: WatchEvent.Kind<out Any>?) {
        when (kind) {
            ENTRY_CREATE -> fileEvents.send(FileEvent(file.canonicalPath, FileEvent.EventType.CREATED))
            ENTRY_MODIFY -> fileEvents.send(FileEvent(file.canonicalPath, FileEvent.EventType.MODIFIED))
            ENTRY_DELETE -> fileEvents.send(FileEvent(file.canonicalPath, FileEvent.EventType.DELETED))
        }
    }

    private fun handleDirectoryEvent(file: File, kind: WatchEvent.Kind<out Any>?) {
        if (kind == ENTRY_DELETE && subDirectoryWatchers.containsKey(file.name)) {
            logger.debug("Directory ${file.name} has been removed")
            subDirectoryWatchers.remove(file.name)
            return
        }

        if (kind == ENTRY_CREATE) {
            logger.debug("Directory ${file.name} added")
            val subDirectoryWatcher = DirectoryWatcher(file.canonicalPath, fileEvents)
            subDirectoryWatchers[file.name] = subDirectoryWatcher
            subDirectoryWatcher.watch()
        }
    }

}