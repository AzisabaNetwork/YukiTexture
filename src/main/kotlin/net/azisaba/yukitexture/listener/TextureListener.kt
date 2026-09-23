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

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val expiredThreshold = 60_000L // 60秒以上経過した未参加エントリを削除
        pendingTextureApplies.entries.removeIf { now - it.value > expiredThreshold }
    }

    @EventHandler
    fun onPreJoin(e: AsyncPlayerPreLoginEvent) {
        cleanupExpired()
        val data = PlayerData.getByUUID(e.uniqueId)
        val shouldApply = if (data != null && data.childServer != null) {
            data.childServer !in plugin.dontApplyTextureServers
        } else {
            true
        }
        if (shouldApply) {
            pendingTextureApplies[e.uniqueId] = System.currentTimeMillis()
        }
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val registered = pendingTextureApplies.remove(e.player.uniqueId)
        if (registered != null && e.player.hasPermission("yukitexture.receive")) {
            plugin.applyTex(e.player)
        }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        pendingTextureApplies.remove(e.player.uniqueId)
    }
}
