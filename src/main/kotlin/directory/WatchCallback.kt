package ch.guengel.imageserver.directory

import java.nio.file.Path

interface WatchCallback {
    fun deleted(path: Path, type: EntryType)
    fun created(path: Path, type: EntryType)
    fun modified(path: Path, type: EntryType)
}