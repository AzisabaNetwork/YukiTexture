package net.azisaba.yukitexture.listener

import net.azisaba.yukitexture.YukiTexture
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class TextureListener(private val plugin: YukiTexture) : Listener {

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        if (!e.player.hasPermission("yukitexture.receive")) return
        plugin.applyTexAsync(e.player)
    }
}
