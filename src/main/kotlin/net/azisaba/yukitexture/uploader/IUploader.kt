package net.azisaba.yukitexture.uploader

import java.nio.file.Path

interface IUploader {
    /**
     * Upload file
     *
     * @param targetFilePath upload file
     * @return if success, sha1 hash (if not provided, return empty).
     */
    suspend fun upload(targetFilePath: Path): Result<String>

    /**
     * Get url for resource pack
     *
     */
    suspend fun getUrl(): Result<String>

    /**
     * When is resource pack last updated at (unix timestamp)
     *
     * @return timestamp. (if not uploaded yet, returns -1)
     */
    fun lastUpdatedAt(): Long
}
