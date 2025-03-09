package net.azisaba.yukitexture.merger

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import net.azisaba.yukitexture.config.S3Config
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration

class ResourcePackMerger {
    suspend fun uploadToS3(
        targetFilePath: Path,
        credentials: S3Config,
    ): Result<Url> {
        val bucketName = credentials.bucketName
        val objectKey = credentials.objectKey // アップロードするファイル名
        val cloudflareEndpoint = credentials.endpoint

        S3Client {
            region = credentials.region
            endpointUrl = Url.parse(cloudflareEndpoint)
            credentialsProvider =
                StaticCredentialsProvider {
                    accessKeyId = credentials.accessKeyId
                    secretAccessKey = credentials.secretAccessKey
                }
        }.use { s3Client ->

            try {
                val putObjectRequest =
                    PutObjectRequest {
                        bucket = bucketName
                        key = objectKey
                        body = ByteStream.fromBytes(Files.readAllBytes(targetFilePath))
                    }

                s3Client.putObject(putObjectRequest)
                println("File uploaded successfully: $objectKey")

                val getObjectRequest =
                    GetObjectRequest {
                        bucket = bucketName
                        key = objectKey
                    }

                // URL を生成
                val presignedUrl =
                    s3Client.presignGetObject(
                        getObjectRequest,
                        duration = Duration.parse("7d"),
                    )
                println("File URL: ${presignedUrl.url}")
                return Result.success(presignedUrl.url)
            } catch (e: Exception) {
                println("Upload failed: ${e.message}")
                return Result.failure(e)
            }
        }
    }
}
