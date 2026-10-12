package net.azisaba.yukitexture.listener

import net.azisaba.yukitexture.PlayerData
import net.azisaba.yukitexture.YukiTexture
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TextureListener(private val plugin: YukiTexture) : Listener {
    private val pendingTextureApplies = ConcurrentHashMap<UUID, Long>()
    private val playerProtocolVersions = ConcurrentHashMap<UUID, Int>()

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val expiredThreshold = 60_000L
        pendingTextureApplies.entries.removeIf { now - it.value > expiredThreshold }
        playerProtocolVersions.keys.removeIf { !pendingTextureApplies.containsKey(it) }
    }

    @EventHandler
    fun onPreJoin(e: AsyncPlayerPreLoginEvent) {
        try {
            cleanupExpired()

            var data = PlayerData.getByUUID(e.uniqueId)
            if (data == null || data.childServer.isNullOrBlank()) {
                for (i in 1..2) {
                    try {
                        Thread.sleep(50L)
                    } catch (_: InterruptedException) {
                        break
                    }
                    val retryData = PlayerData.getByUUID(e.uniqueId)
                    if (retryData != null && !retryData.childServer.isNullOrBlank()) {
                        data = retryData
                        break
                    }
                }
            }

            val protocolVersion = data?.protocolVersion
            if (protocolVersion != null) {
                playerProtocolVersions[e.uniqueId] = protocolVersion
                plugin.logger.info("[PreJoin] Player ${e.name} (${e.uniqueId}): Redis から protocolVersion=$protocolVersion をキャッシュしました。")
            } else {
                plugin.logger.info("[PreJoin] Player ${e.name} (${e.uniqueId}): Redis に protocolVersion はありませんでした。")
            }

            val childServer = data?.childServer?.trim()
            val shouldApply = if (!childServer.isNullOrEmpty()) {
                !plugin.dontApplyTextureServers.contains(childServer)
            } else {
                true
            }

            if (childServer != null) {
                if (shouldApply) {
                    plugin.logger.info("Player ${e.name} (${e.uniqueId}) connected from '$childServer'. Applying texture pack.")
                } else {
                    plugin.logger.info("Player ${e.name} (${e.uniqueId}) connected from '$childServer' (matched dontApplyTexture). Skipping texture pack.")
                }
            } else {
                plugin.logger.info("Player ${e.name} (${e.uniqueId}) connected (no childServer found in Redis). Applying texture pack.")
            }

            if (shouldApply) {
                pendingTextureApplies[e.uniqueId] = System.currentTimeMillis()
            }
        } catch (t: Throwable) {
            plugin.logger.warning("[PreJoin] onPreJoin処理中にエラーが発生しました: ${t.message}")
            t.printStackTrace()
            pendingTextureApplies[e.uniqueId] = System.currentTimeMillis()
        }
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val registered = pendingTextureApplies.remove(e.player.uniqueId)
        val cachedProtocol = playerProtocolVersions.remove(e.player.uniqueId)
        plugin.logger.info("[PlayerJoin] Player ${e.player.name} (${e.player.uniqueId}): registered=$registered, cachedProtocol=$cachedProtocol, hasPermission=${e.player.hasPermission("yukitexture.receive")}")
        if (registered != null && e.player.hasPermission("yukitexture.receive")) {
            plugin.applyTex(e.player, cachedProtocol)
        }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        pendingTextureApplies.remove(e.player.uniqueId)
        playerProtocolVersions.remove(e.player.uniqueId)
    }
}
