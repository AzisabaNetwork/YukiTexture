package net.azisaba.yukitexture.config

import kotlinx.serialization.Serializable

@Serializable
data class SecretConfig(
    val s3: S3Config = S3Config(),
)

@Serializable
data class S3Config(
    val bucketName: String = "example-bucket",
    val objectKey: String = "resourcepack.zip",
    val endpoint: String = "endpoint.is.here",
    val region: String = "us-east-1",
    val accessKeyId: String = "accessKeyId",
    val secretAccessKey: String = "secretAccessKey",
)
