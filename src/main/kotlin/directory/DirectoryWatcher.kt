package ch.guengel.imageserver.directory

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.*
import kotlin.coroutines.CoroutineContext

class DirectoryWatcher(private val root: Path, private val callback: WatchCallback) : AutoCloseable {
    private var watchJob: Job? = null

    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val keyMap = mutableMapOf<WatchKey, Path>()
    private val directorySet = mutableSetOf<Path>()

    fun start() {
        if (watchJob != null) {
            logger.warn("DirectoryWatcher for {} already started", root.toAbsolutePath())
            return
        }

        registerDirectory(root)
        scanRootForDirectories(root)

        watchJob = GlobalScope.launch {
            watch(coroutineContext)
        }
        logger.debug("DirectoryWatcher for {} started", root.toAbsolutePath())
    }

    private fun scanRootForDirectories(root: Path) {
        Files.walkFileTree(root, setOf(FileVisitOption.FOLLOW_LINKS),
            MAX_DEPTH, object : SimpleFileVisitor<Path>() {
                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    registerDirectory(dir)
                    return FileVisitResult.CONTINUE
                }
            })
    }

    private fun registerDirectory(directory: Path) {
        val key: WatchKey = directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )
        keyMap[key] = directory
        directorySet.add(directory)
        logger.debug("Registered {} for watching", directory.toString())
    }

    private fun unregisterDirectory(watchKey: WatchKey) {
        keyMap.remove(watchKey)?.also {
            directorySet.remove(it)
            logger.info("Stop watching directory {}", it.toString())
        }
    }

    private fun watch(coroutineContext: CoroutineContext) {
        while (coroutineContext.isActive) {
            waitForFileSystemEvent()?.let {
                it.handleEvents()
                if (!it.reset()) {
                    unregisterDirectory(it)
                }
            }
        }
    }

    private fun waitForFileSystemEvent(): WatchKey? {
        var watchKey: WatchKey? = null
        try {
            watchKey = watchService.take()
        } catch (e: InterruptedException) {
            logger.error("Interrupted while waiting", e)
        } catch (e: ClosedWatchServiceException) {
            logger.debug("watch service is closed")
        }
        return watchKey
    }

    private fun WatchKey.handleEvents() {
        pollEvents().forEach {
            val kind = it.kind()
            if (kind == StandardWatchEventKinds.OVERFLOW) {
                logger.error("Watch overflow")
                return@forEach
            }

            resolvePath(this, it)?.let {
                if (Files.isDirectory(it)) {
                    registerDirectory(it)
                }

                when (kind) {
                    StandardWatchEventKinds.ENTRY_CREATE -> callback.created(it, it.toEntryType())
                    StandardWatchEventKinds.ENTRY_MODIFY -> callback.modified(it, it.toEntryType())
                    StandardWatchEventKinds.ENTRY_DELETE -> callback.deleted(it, it.toEntryType())
                }
            }
        }
    }

    private fun Path.toEntryType(): EntryType = if (Files.isDirectory(this) || this in directorySet) {
        EntryType.DIRECTORY
    } else {
        EntryType.FILE
    }

    private fun resolvePath(watchKey: WatchKey, event: WatchEvent<*>): Path? {
        val child = pathFromWatchEvent(event)
        val parent = keyMap[watchKey]
        return parent?.resolve(child)
    }

    @Suppress("UNCHECKED_CAST")
    private fun pathFromWatchEvent(event: WatchEvent<*>): Path = (event as WatchEvent<Path>).context()

    override fun close() {
        logger.debug("Stopping watch service")
        watchService.close()
        runBlocking { watchJob?.cancelAndJoin() }
        watchJob = null
    }

    private companion object {
        val logger = LoggerFactory.getLogger(DirectoryWatcher::class.java)
        const val MAX_DEPTH = 100
    }
}