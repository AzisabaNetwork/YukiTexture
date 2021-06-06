package net.azisaba.yukitexture.listener

import net.azisaba.yukitexture.YukiTexture
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class TextureListener(private val plugin: YukiTexture) : Listener {

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        if (e.player.hasPermission("yukitexture.receive") && plugin.tex.isNotBlank()) {
            plugin.server.scheduler.runTaskLaterAsynchronously(plugin, {
                plugin.db.let {
                    if (it == null) {
                        plugin.applyTex(e.player)
                        return@let
                    }
                    val player = it.findOne(
                        "SELECT `last_server` FROM `${plugin.dbPrefix}players` WHERE `uuid` = ?",
                        e.player.uniqueId.toString(),
                        //plugin.serverName,
                    )
                    if (
                        player == null
                        || player["last_server"] == null
                        || !plugin.getTextureConfig().getStringList("dontApplyTexture").contains(player["last_server"])
                    ) {
                        plugin.applyTex(e.player)
                    }
                }
            }, 1)
        }
        plugin.db?.let {
            plugin.server.scheduler.runTaskLaterAsynchronously(plugin, {
                it.execute(
                    "INSERT INTO `${plugin.dbPrefix}players` (`uuid`, `last_server`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `last_server` = ?, `pending_quit` = 0",
                    e.player.uniqueId.toString(),
                    plugin.serverName,
                    plugin.serverName,
                )
            }, plugin.getTextureConfig().getLong("delay", 5))
        }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        plugin.db?.let {
            plugin.server.scheduler.runTaskAsynchronously(plugin) {
                it.execute(
                    "INSERT INTO `${plugin.dbPrefix}players` (`uuid`, `last_server`, `pending_quit`) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE `last_server` = ?, `pending_quit` = 1",
                    e.player.uniqueId.toString(),
                    plugin.serverName,
                    plugin.serverName,
                )
            }
            plugin.server.scheduler.runTaskLaterAsynchronously(plugin, {
                val player = it.findOne(
                    "SELECT `uuid` FROM `${plugin.dbPrefix}players` WHERE `uuid` = ? AND `last_server` = ? AND `pending_quit` = 1",
                    e.player.uniqueId.toString(),
                    plugin.serverName,
                )
                if (player != null) {
                    it.execute(
                        "UPDATE `${plugin.dbPrefix}players` SET `last_server` = NULL, `pending_quit` = 0 WHERE `uuid` = ?",
                        e.player.uniqueId.toString(),
                    )
                }
            }, 20) // around 1s
        }
    }
}
