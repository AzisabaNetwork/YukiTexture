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

        var data = PlayerData.getByUUID(e.uniqueId)
        // Velocity側の非同期タスクによる childServer 更新との競合対策として、最大2回リトライ
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

        val childServer = data?.childServer?.trim()
        val shouldApply = if (childServer != null && childServer.isNotEmpty()) {
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
