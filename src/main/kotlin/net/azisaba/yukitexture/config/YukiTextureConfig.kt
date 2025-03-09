package net.azisaba.yukitexture.config

import kotlinx.serialization.Serializable

@Serializable
data class YukiTextureConfig(
    val packUrl: String = "https://packs.example.com/resourcepack.zip",
    val dontApplyTexture: List<String> = listOf("child-server-1", "child-server-2"),
    val redis: RedisConfig = RedisConfig(),
    val uploader: UploaderConfig = UploaderConfig(),
)

@Serializable
data class RedisConfig(
    val host: String = "host",
    val port: Int = 6379,
    val user: String = "user",
    val password: String = "password",
)

@Serializable
data class UploaderConfig(
    val useUploader: Boolean = false,
    val uploaderType: String = "s3",
)
