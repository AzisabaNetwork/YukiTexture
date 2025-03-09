package net.azisaba.yukitexture.merger

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.azisaba.yukitexture.util.ZipUtil
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePackMerger(
    private val tempFolder: File,
) {
    fun mergeAndZip(targetDataList: List<File>): File {
        val folders: MutableList<File> = mutableListOf()

        for (targetData in targetDataList) {
            if (targetData.isDirectory) {
                folders.add(targetData)
            } else {
                when (targetData.extension) {
                    "zip" -> {
                        val unzippedTargetData = File(tempFolder, targetData.nameWithoutExtension)
                        ZipUtil.unzip(
                            targetData.toPath(),
                            unzippedTargetData.toPath(),
                        )
                        folders.add(unzippedTargetData)
                    }
                    else -> {
                        // Unsupported file
                        println("This file is unsupported. file: ${targetData.canonicalPath}")
                    }
                }
            }
        }

        // create output folder
        val outputTempFolder = File(tempFolder, "output")
        if (outputTempFolder.exists()) {
            outputTempFolder.deleteRecursively()
            outputTempFolder.mkdirs()
        }

        // merge all folders
        mergeAllFolders(folders, outputTempFolder)

        // create a zip file
        val zipFile = File(tempFolder, "output.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            outputTempFolder
                .walk(FileWalkDirection.BOTTOM_UP)
                .sortedBy { it.isDirectory }
                .toList()
                .forEachIndexed { i, file ->
                    val zipFileName =
                        file.absolutePath
                            .removePrefix(outputTempFolder.absolutePath)
                            .removePrefix("/")
                    val zipEntry = ZipEntry("$zipFileName${if (file.isDirectory) "/" else ""}")
                    zipOut.putNextEntry(zipEntry)
                    if (file.isFile) {
                        file.inputStream().copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
        }

        // return a zip file
        return zipFile
    }

    private fun mergeAllFolders(
        folders: List<File>,
        outputFolder: File,
    ) {
        folders.forEachIndexed { i, folder ->
            val parentFolder = File(folder, "assets")
            recursiveCopy(parentFolder, outputFolder)
        }
    }

    private fun recursiveCopy(
        from: File,
        to: File,
    ) {
        if (from.isDirectory) {
            // is folder
            val sourceFolder = File(to, from.name)
            sourceFolder.mkdir()
            println("Searching $from")

            // file walk
            from.listFiles().forEach {
                recursiveCopy(it, sourceFolder)
            }
        } else {
            // is non folder
            copy(from, to)
        }
    }

    /**
     * Copy file (auto-fix conflicts)
     *
     * @param fromFile source file
     * @param to destination file
     */
    private fun copy(
        fromFile: File,
        to: File,
    ) {
        val toFile = File(to, fromFile.name)

        // normal copy
        if (!toFile.exists()) {
            Files.copy(fromFile.toPath(), toFile.toPath())
            return
        }

        // conflict copy
        if (fromFile.extension != "json") {
            println("Can't merge file automatically. path: ${fromFile.absoluteFile}")
            return
        }

        toFile.writeText(
            Gson().toJson(
                mergeJsonObject(
                    JsonParser.parseReader(fromFile.reader()).asJsonObject,
                    JsonParser.parseReader(toFile.reader()).asJsonObject,
                ),
            ),
        )
    }

    private fun mergeJsonObject(
        baseJson: JsonObject,
        addonJson: JsonObject,
    ): JsonObject {
        val base = baseJson.deepCopy()
        addonJson.keySet().forEach { key ->
            val baseValue = base.get(key)
            val addonValue = addonJson.get(key)

            // if not exists
            if (!base.has(key)) {
                base.add(key, addonValue)
                return@forEach
            }

            // if exists
            if (baseValue.isJsonObject && addonValue.isJsonObject) {
                // merge object
                base.add(
                    key,
                    mergeJsonObject(
                        baseValue.asJsonObject,
                        addonValue.asJsonObject,
                    ),
                )
            } else if (baseValue.isJsonArray && addonValue.isJsonArray) {
                // merge arrays
                base.add(
                    key,
                    JsonArray().apply {
                        addAll(baseValue.asJsonArray)
                        addAll(addonValue.asJsonArray)
                    },
                )
            } else {
                // overwrite value
                base.add(key, addonValue)
            }
        }
        return base
    }
}
