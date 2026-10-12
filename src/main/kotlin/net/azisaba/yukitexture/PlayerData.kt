package net.azisaba.yukitexture

import com.google.gson.Gson
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    val username: String,
    val childServer: String?,
    val protocolVersion: Int? = null,
    val rawJson: String? = null
) {
    companion object {
        private val gson = Gson()

        fun getByUUID(uuid: UUID): PlayerData? {
            val plugin = try { JavaPlugin.getPlugin(YukiTexture::class.java) } catch (_: Throwable) { null }
            val jedisBox = plugin?.jedisBox ?: return null
            return try {
                val rawData = jedisBox.client.get("velocity-redis-bridge:player:$uuid") ?: return null

                val jsonObject = gson.fromJson(rawData, com.google.gson.JsonObject::class.java)
                val parsedData = gson.fromJson(jsonObject, PlayerData::class.java)

                val resolvedProtocol = parsedData.protocolVersion
                    ?: jsonObject.get("protocol")?.takeIf { it.isJsonPrimitive }?.let { if (it.asJsonPrimitive.isNumber) it.asInt else VersionPack.parseProtocol(it.asString) }
                    ?: jsonObject.get("clientProtocol")?.takeIf { it.isJsonPrimitive }?.let { if (it.asJsonPrimitive.isNumber) it.asInt else VersionPack.parseProtocol(it.asString) }
                    ?: jsonObject.get("clientVersion")?.takeIf { it.isJsonPrimitive }?.let { if (it.asJsonPrimitive.isNumber) it.asInt else VersionPack.parseProtocol(it.asString) }
                    ?: jsonObject.get("version")?.takeIf { it.isJsonPrimitive }?.let { if (it.asJsonPrimitive.isNumber) it.asInt else VersionPack.parseProtocol(it.asString) }

                parsedData.copy(protocolVersion = resolvedProtocol, rawJson = rawData)
            } catch (e: Exception) {
                plugin.logger.warning("[Redis] PlayerDataパースエラー: ${e.message}")
                null
            }
        }
    }
}