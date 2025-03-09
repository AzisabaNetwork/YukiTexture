package net.azisaba.yukitexture

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.result.Result
import net.azisaba.yukitexture.command.ReloadTextureCommand
import net.azisaba.yukitexture.command.TextureCommand
import net.azisaba.yukitexture.config.ConfigUtil
import net.azisaba.yukitexture.config.SecretConfig
import net.azisaba.yukitexture.config.YukiTextureConfig
import net.azisaba.yukitexture.listener.TextureListener
import net.azisaba.yukitexture.redis.JedisBox
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.apache.commons.codec.digest.DigestUtils
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import org.bukkit.ChatColor as CC

class YukiTexture : JavaPlugin() {
    private val prefix = "${CC.GRAY}[${CC.RED}$name${CC.GRAY}]${CC.RESET}"

    /**
     * Texture pack URL
     */
    lateinit var tex: String

    /**
     * SHA-1 hash
     * null means undefined, and the resource pack needs to be downloaded before sending a request to a client.
     */
    private var sha1: String? = null

    var jedisBox: JedisBox? = null

    internal lateinit var yukiConfig: YukiTextureConfig

    internal lateinit var secretConfig: SecretConfig

    fun applyTex(player: Player) {
        if (tex.isBlank()) return

        // update sha1 hash of resource pack only if sha1 hash is not calculated yet
        // but disable this for now
//        if (true || sha1 === null) {
        val (_, response, result) =
            FuelManager()
                .addRequestInterceptor { next: (Request) -> Request ->
                    { req: Request ->
                        player.sendActionBar(Component.text("${req.url.host} に接続中..."))
                        next(req)
                    }
                }.get(tex)
                .responseProgress { readBytes, totalBytes ->
                    val percent = readBytes.toFloat().div(totalBytes).times(100)
                    player.sendActionBar(Component.text("リソースパックをダウンロード中... ($percent %)"))
                }.response()
        val joinedHeaders =
            response.headers
                .entries
                .joinToString("\n") {
                    "${CC.AQUA}${it.key}: ${CC.RESET}${it.value.joinToString(" ")}"
                }
        player.sendMessage(
            Component
                .text("$prefix レスポンスは ")
                .append(
                    Component
                        .text("${response.statusCode} (${response.responseMessage})")
                        .hoverEvent(HoverEvent.showText(Component.text("${CC.YELLOW}URL: ${CC.RESET}${response.url}\n$joinedHeaders"))),
                ).append(Component.text("です。")),
        )
        if (result is Result.Failure) {
            result.getException().printStackTrace()
            return
        }
        sha1 = DigestUtils.sha1Hex(result.get())
//        }
        player.sendTitle("", "プレイヤーのリソースパックを変更中...", 0, 100, 20)
        player.setResourcePack(tex, sha1 ?: "")
        player.sendMessage(
            Component
                .text(prefix)
                .append(
                    Component
                        .text("${CC.GREEN}完了しました。")
                        .hoverEvent(HoverEvent.showText(Component.text("SHA-1: $sha1"))),
                ),
        )
    }

    override fun onEnable() {
        val configFile =
            File(dataFolder, "config.yml").also {
                ConfigUtil.saveConfig(YukiTextureConfig(), it)
            }
        val secretFile =
            File(dataFolder, "secret.yml").also {
                ConfigUtil.saveConfig(SecretConfig(), it)
            }

        // load configurations
        yukiConfig = ConfigUtil.loadConfig(YukiTextureConfig.serializer(), configFile)
        secretConfig = ConfigUtil.loadConfig(SecretConfig.serializer(), secretFile)

        val redis = yukiConfig.redis
        val host = redis.host
        val port = redis.port
        val user = redis.user
        val password = redis.password

        try {
            logger.info("Trying $host:$port...")
            jedisBox = JedisBox(host, port, user, password)
            jedisBox?.jedisPool?.resource?.use { it.get("something") }
            logger.info("Redisに接続しました。")
        } catch (e: Exception) {
            logger.warning("Redisに接続できませんでした。データベースなしで続行します。")
            e.printStackTrace()
            jedisBox = null
        }

        getCommand("tex")?.setExecutor(TextureCommand(this))
        getCommand("reloadtex")?.setExecutor(ReloadTextureCommand(this))
        server.pluginManager.registerEvents(TextureListener(this), this)
    }

    override fun onDisable() {
        server.messenger.unregisterOutgoingPluginChannel(this)
        server.messenger.unregisterIncomingPluginChannel(this)
    }
}
