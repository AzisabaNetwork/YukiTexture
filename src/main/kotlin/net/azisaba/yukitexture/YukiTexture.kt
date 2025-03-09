package net.azisaba.yukitexture

import co.aikar.commands.PaperCommandManager
import kotlinx.coroutines.runBlocking
import net.azisaba.yukitexture.command.ReloadTextureCommand
import net.azisaba.yukitexture.command.TextureCommand
import net.azisaba.yukitexture.command.YukiTextureCommand
import net.azisaba.yukitexture.config.ConfigUtil
import net.azisaba.yukitexture.config.SecretConfig
import net.azisaba.yukitexture.config.YukiTextureConfig
import net.azisaba.yukitexture.extension.registerEvents
import net.azisaba.yukitexture.listener.TextureListener
import net.azisaba.yukitexture.merger.ResourcePackMerger
import net.azisaba.yukitexture.redis.JedisBox
import net.azisaba.yukitexture.uploader.S3Uploader
import net.azisaba.yukitexture.uploader.UploaderManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.apache.commons.codec.digest.DigestUtils
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.bukkit.ChatColor as CC

class YukiTexture : JavaPlugin() {
    private val prefix = "${CC.GRAY}[${CC.RED}$name${CC.GRAY}]${CC.RESET}"

    /**
     * Http client to get resource pack from web storage
     */
    internal val httpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()

    internal var jedisBox: JedisBox? = null

    /**
     * Name of uploader to store resource pack
     */
    internal var uploaderName = ""

    internal lateinit var yukiConfig: YukiTextureConfig

    internal lateinit var secretConfig: SecretConfig

    internal lateinit var resourcePackMerger: ResourcePackMerger

    internal lateinit var configFile: File

    internal lateinit var secretFile: File

    internal lateinit var commandManager: PaperCommandManager

    private var initialized = false

    override fun onEnable() {
        if (!dataFolder.exists()) dataFolder.mkdirs()

        // configurations
        configFile =
            File(dataFolder, "config.yml").also {
                ConfigUtil.saveConfig(YukiTextureConfig(), it)
            }
        secretFile =
            File(dataFolder, "secret.yml").also {
                ConfigUtil.saveConfig(SecretConfig(), it)
            }
        yukiConfig = ConfigUtil.loadConfig(YukiTextureConfig.serializer(), configFile)
        secretConfig = ConfigUtil.loadConfig(SecretConfig.serializer(), secretFile)

        // connect redis
        val redis = yukiConfig.redis
        try {
            logger.info("Trying ${redis.host}:${redis.port}...")
            jedisBox = JedisBox(redis.host, redis.port, redis.user, redis.password)
            jedisBox?.jedisPool?.resource?.use { it.get("something") }
            logger.info("Redisに接続しました。")
        } catch (e: Exception) {
            logger.warning("Redisに接続できませんでした。データベースなしで続行します。")
            e.printStackTrace()
        }

        // commands
        getCommand("tex")?.setExecutor(TextureCommand(this))
        getCommand("reloadtex")?.setExecutor(ReloadTextureCommand(this))

        commandManager = PaperCommandManager(this)
        commandManager.registerCommand(YukiTextureCommand(this))

        // event listeners
        registerEvents(TextureListener(this))

        // register uploader
        UploaderManager.registerUploader(
            "s3",
            S3Uploader(secretConfig.s3),
        )

        // set s3 as default uploader
        uploaderName = yukiConfig.uploader.uploaderType

        resourcePackMerger =
            File(dataFolder, "temp").run {
                mkdirs()
                ResourcePackMerger(File(dataFolder, "temp"))
            }

        initialized = true
    }

    override fun onDisable() {
        server.messenger.unregisterOutgoingPluginChannel(this)
        server.messenger.unregisterIncomingPluginChannel(this)

        if (initialized) {
            commandManager.unregisterCommands()
        }
        initialized = false
    }

    override fun reloadConfig() {
        yukiConfig = ConfigUtil.loadConfig(YukiTextureConfig.serializer(), configFile)
        secretConfig = ConfigUtil.loadConfig(SecretConfig.serializer(), secretFile)
    }

    fun applyTex(player: Player) {
        val textureUrl: String =
            if (yukiConfig.uploader.useUploader) {
                runBlocking {
                    UploaderManager.getUrl(uploaderName).fold({
                        return@runBlocking it
                    }) {
                        player.sendMessage(
                            Component.text("テクスチャのURL取得に失敗しました。運営にお問い合わせください。").color(
                                NamedTextColor.RED,
                            ),
                        )
                        error("Failed to get url from uploader $it")
                    }
                }
            } else {
                yukiConfig.packUrl
            }

        var textureHash = ""
        httpClient
            .send(
                HttpRequest.newBuilder(URI.create(textureUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream(),
            ).also {
                if (it.statusCode() != 200) {
                    player.sendMessage(Component.text("テクスチャの取得に失敗しました。運営にお問い合わせください。"))
                    logger.info("Failed to request. status code: ${it.statusCode()}")
                    return
                }

                textureHash = DigestUtils.sha1Hex(it.body())
            }

        player.sendTitle("", "プレイヤーのリソースパックを変更中...", 0, 100, 20)
        player.setResourcePack(textureUrl.toString(), textureHash)

        // complete message
        player.sendMessage(
            Component
                .text(prefix)
                .append(
                    Component
                        .text("${CC.GREEN}完了しました。")
                        .hoverEvent(HoverEvent.showText(Component.text("SHA-1: $textureHash"))),
                ),
        )
    }
}
