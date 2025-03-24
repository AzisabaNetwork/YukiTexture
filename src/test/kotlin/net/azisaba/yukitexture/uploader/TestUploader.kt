package net.azisaba.yukitexture.uploader

import java.nio.file.Path

class TestUploader : IUploader {
    override suspend fun upload(targetFilePath: Path): Result<String> = Result.success("")

    override suspend fun getUrl(): Result<String> = Result.success("")

    override fun lastUpdatedAt(): Long = 0
}
