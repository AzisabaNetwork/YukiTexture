package net.azisaba.yukitexture.model

import com.google.gson.Gson
import net.azisaba.yukitexture.YukiTexture
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
            try {
                plugin.jedisBox.also {
                    (it ?: return null).jedisPool.resource.use { jedis ->
                        val rawData = jedis.get("velocity-redis-bridge:player:$uuid") ?: return null
                        return gson.fromJson(rawData, PlayerData::class.java)
                    }
                }
            } catch (_: Exception) {
                return null
            }
        }
    }
}
