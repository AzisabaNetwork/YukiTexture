package net.azisaba.yukitexture.util

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.math.floor

object ZipUtil {
    fun zip(
        files: List<File>,
        outputZipFile: File,
    ): File {
        ZipOutputStream(FileOutputStream(outputZipFile)).use { zipOut ->
            files.forEachIndexed { i, file ->
                FileInputStream(file).use { fileIn ->
                    val zipEntry = ZipEntry(file.canonicalPath)
                    zipOut.putNextEntry(zipEntry)
                    fileIn.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
        return outputZipFile
    }

    @Throws(IOException::class)
    fun unzip(
        zipFilePath: Path,
        destDirectory: Path,
    ) {
        if (!destDirectory.exists()) destDirectory.createDirectories()

        ZipFile(zipFilePath.absolutePathString()).use { zip ->
            zip.entries().toList().also { entries ->
                var beforeProgress = 0
                entries.forEachIndexed { i, entry ->
                    zip.getInputStream(entry).use { input ->
                        val filePath = destDirectory.resolve(entry.name)
                        if (!entry.isDirectory) {
                            extractFile(input, filePath)
                        } else {
                            filePath.createDirectories()
                        }
                    }
                    val progress = floor(i.toDouble() / entries.size * 10).toInt()
                    if (progress > beforeProgress) {
                        println("Progress: ${progress * 10}%...")
                        beforeProgress = progress
                    }
                }
            }
        }
        println("${zipFilePath.absolutePathString()} was extracted to ${destDirectory.absolutePathString()}")
    }

    @Throws(IOException::class)
    private fun extractFile(
        inputStream: InputStream,
        destFilePath: Path,
    ) {
        BufferedOutputStream(FileOutputStream(destFilePath.absolutePathString())).use { bos ->
            var read: Int
            val bytesIn = ByteArray(4096)
            while (inputStream.read(bytesIn).also { read = it } != -1) {
                bos.write(bytesIn, 0, read)
            }
        }
    }
}
