package ch.guengel.imageserver.directory

import assertk.assertThat
import assertk.assertions.isGreaterThan
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class DirectoryListerTest {

    @Test
    fun getAllFiles() {
        val directoryLister = DirectoryLister(Path.of("."), Regex(".*"))
        val files = directoryLister.getFiles()

        assertThat(files.size).isGreaterThan(20)
    }

    @Test
    fun getFilteredFiles() {
        val directoryLister =
            DirectoryLister(Path.of("."), Regex(".*(?:\\.gradle|\\.kt)$"))
        val files = directoryLister.getFiles()

        assertThat(files.size).isGreaterThan(4)
    }


}