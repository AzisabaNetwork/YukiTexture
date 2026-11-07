package net.azisaba.yukitexture.listener

import net.azisaba.yukitexture.PlayerData
import net.azisaba.yukitexture.YukiTexture
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TextureListener(private val plugin: YukiTexture) : Listener {
    private val pendingTextureApplies = ConcurrentHashMap<UUID, Long>()
    private val playerProtocolVersions = ConcurrentHashMap<UUID, Int>()
    private val retryCounts = ConcurrentHashMap<UUID, Int>()

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
            val listed = !childServer.isNullOrEmpty() && plugin.dontApplyTextureServers.contains(childServer)
            // 除外リストのサーバからの移動でも、現在の接続でパックの適用に成功していなければ送信する
            val alreadyLoaded = listed && plugin.hasLoadedInCurrentSession(e.uniqueId, data)
            val shouldApply = !alreadyLoaded
            if (listed && !alreadyLoaded) {
                plugin.logger.info("Player ${e.name}: '$childServer' は除外リストですが、現在の接続でリソースパックの適用成功が確認できないため送信します。")
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
        if (registered != null && System.currentTimeMillis() - registered <= EXPIRE_MILLIS && e.player.hasPermission("yukitexture.receive")) {
            // Join直後はクライアントが地形読み込み中で、リソパの適用が進まないことがあるため少し遅らせて送信する
            val player = e.player
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (player.isOnline) plugin.applyTex(player, cachedProtocol)
            }, APPLY_DELAY_TICKS)
        } else if (registered != null) {
            plugin.logger.info("[PlayerJoin] Player ${e.player.name}: 登録が期限切れまたは権限なしのため送信しません。")
        }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        // 再接続時、古いセッションのQuitが新しい接続の登録(PreLogin)を消してしまうため、pending は削除しない。
        // 期限切れは cleanupExpired / onJoin の時刻チェックで処理する。
        retryCounts.remove(e.player.uniqueId)
        plugin.forgetPlayer(e.player.uniqueId)
    }

    /**
     * クライアントからの受信状況をログに残し、ダウンロード/再読み込み失敗時は限定回数だけ再送する。
     */
    @EventHandler
    fun onResourcePackStatus(e: PlayerResourcePackStatusEvent) {
        val player = e.player
        plugin.logger.info("[PackStatus] ${player.name}: ${e.status}")
        when (e.status) {
            Status.SUCCESSFULLY_LOADED -> {
                retryCounts.remove(player.uniqueId)
                plugin.markLoaded(player.uniqueId)
            }
            Status.FAILED_DOWNLOAD, Status.FAILED_RELOAD -> {
                val count = retryCounts.merge(player.uniqueId, 1) { a, b -> a + b } ?: 1
                if (count <= MAX_RETRIES) {
                    plugin.logger.warning("[PackStatus] ${player.name}: 失敗したため再送します ($count/$MAX_RETRIES)")
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        if (player.isOnline) plugin.applyTex(player, force = true)
                    }, RETRY_DELAY_TICKS)
                } else {
                    plugin.logger.warning("[PackStatus] ${player.name}: 再送の上限に達しました。")
                }
            }
            else -> {}
        }
    }

    companion object {
        private const val EXPIRE_MILLIS = 60_000L
        private const val APPLY_DELAY_TICKS = 20L
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_TICKS = 60L
    }
}
