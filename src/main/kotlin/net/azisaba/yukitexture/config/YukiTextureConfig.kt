package net.azisaba.yukitexture.config

import kotlinx.serialization.Serializable

@Serializable
data class YukiTextureConfig(
    val redis: RedisConfig = RedisConfig(),
    val dontApplyTexture: List<String> = listOf("child-server-1", "child-server-2"),
)

@Serializable
data class RedisConfig(
    val host: String = "host",
    val port: Int = 6379,
    val user: String = "user",
    val password: String = "password",
)
