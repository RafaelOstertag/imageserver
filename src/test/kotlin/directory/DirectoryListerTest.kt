package ch.guengel.imageserver.directory

import assertk.assertThat
import assertk.assertions.isGreaterThan
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class DirectoryListerTest {

    @Test
    fun getAllFiles() {
        val directoryLister = DirectoryLister(Path.of("."), Regex(".*"))
        val files: List<Path> = runBlocking {
            val fileList = mutableListOf<Path>()
            for (file in directoryLister.getFiles()) {
                fileList.add(file)
            }
            fileList
        }

        assertThat(files.size).isGreaterThan(20)
    }

    @Test
    fun getFilteredFiles() {
        val directoryLister =
            DirectoryLister(Path.of("."), Regex(".*(?:\\.gradle|\\.kt)$"))
        val files: List<Path> = runBlocking {
            val fileList = mutableListOf<Path>()
            for (path in directoryLister.getFiles()) {
                fileList.add(path)
            }
            fileList
        }

        assertThat(files.size).isGreaterThan(4)
    }


}