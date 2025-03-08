package net.azisaba.yukitexture.config

import kotlinx.serialization.Serializable

@Serializable
data class YukiTextureConfig(
    val redis: RedisConfig = RedisConfig(),
)

@Serializable
data class RedisConfig(
    val host: String = "host",
    val port: Int = 6379,
    val user: String = "user",
    val password: String = "password",
)
