package net.azisaba.yukitexture

import com.google.gson.Gson
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    val username: String,
    val childServer: String?,
    val protocolVersion: Int? = null
) {
    companion object {
        private val gson = Gson()

        fun getByUUID(uuid: UUID): PlayerData? {
            return try {
                val plugin = JavaPlugin.getPlugin(YukiTexture::class.java)
                val jedisBox = plugin.jedisBox ?: return null
                val rawData = jedisBox.client.get("velocity-redis-bridge:player:$uuid") ?: return null
                gson.fromJson(rawData, PlayerData::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }
}