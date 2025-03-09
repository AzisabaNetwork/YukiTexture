package net.azisaba.yukitexture.uploader

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.smithy.kotlin.runtime.content.ByteStream
import net.azisaba.yukitexture.config.S3Config
import net.azisaba.yukitexture.extension.of
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration

class S3Uploader(
    private val credentials: S3Config,
) : IUploader {
    private var lastUpdated: Long = -1

    override suspend fun upload(targetFilePath: Path): Result<String> {
        S3Client.Companion.of(credentials).use { s3Client ->
            try {
                val putObjectRequest =
                    PutObjectRequest.Companion {
                        bucket = credentials.bucketName
                        key = credentials.objectKey
                        body = ByteStream.Companion.fromBytes(Files.readAllBytes(targetFilePath))
                    }

                val response = s3Client.putObject(putObjectRequest)

                lastUpdated = System.currentTimeMillis()
                return Result.success(response.checksumSha1 ?: "")
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
    }

    override suspend fun getUrl(): Result<String> {
        S3Client.Companion.of(credentials).use { s3Client ->
            try {
                val getObjectRequest =
                    GetObjectRequest.Companion {
                        bucket = credentials.bucketName
                        key = credentials.objectKey
                    }

                val presignedUrl =
                    s3Client.presignGetObject(
                        getObjectRequest,
                        duration = URL_DURATION,
                    )
                return Result.success(presignedUrl.url.toString())
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
    }

    override fun lastUpdatedAt(): Long = lastUpdated

    companion object {
        private val URL_DURATION = Duration.Companion.parse("5m")
    }
}
