package net.azisaba.yukitexture.listener

import net.azisaba.yukitexture.PlayerData
import net.azisaba.yukitexture.YukiTexture
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.util.Collections
import java.util.UUID

class TextureListener(private val plugin: YukiTexture) : Listener {
    private val applyTextureLater = Collections.synchronizedSet(mutableSetOf<UUID>())

    @EventHandler
    fun onPreJoin(e: AsyncPlayerPreLoginEvent) {
        val data = PlayerData.getByUUID(e.uniqueId)
        //println("Server of ${data?.username}: " + data?.childServer)
        if (data != null) {
            if (!plugin.getTextureConfig().getStringList("dontApplyTexture").contains(data.childServer)) {
                applyTextureLater.add(e.uniqueId)
            }
        } else {
            applyTextureLater.add(e.uniqueId)
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
            applyTextureLater.remove(e.uniqueId)
        }, 20L)
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        if (e.player.hasPermission("yukitexture.receive") && applyTextureLater.contains(e.player.uniqueId)) {
            plugin.applyTex(e.player)
        }
    }
}
