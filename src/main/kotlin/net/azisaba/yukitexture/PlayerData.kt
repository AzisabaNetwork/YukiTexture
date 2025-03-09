package net.azisaba.yukitexture

import com.google.gson.Gson
import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    val username: String,
    val childServer: String?,
) {
    companion object {
        private val gson = Gson()

        fun getByUUID(
            plugin: YukiTexture,
            uuid: UUID,
        ): PlayerData? {
            plugin.jedisBox.also {
                (it ?: return null).jedisPool.resource.use { jedis ->
                    val rawData = jedis.get("velocity-redis-bridge:player:$uuid") ?: return null
                    return gson.fromJson(rawData, PlayerData::class.java)
                }
            }
        }
    }
}
