package net.azisaba.yukitexture

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.result.Result
import me.rayzr522.jsonmessage.JSONMessage
import net.azisaba.yukitexture.command.ReloadTextureCommand
import net.azisaba.yukitexture.command.TextureCommand
import net.azisaba.yukitexture.listener.TextureListener
import net.azisaba.yukitexture.sql.DBConnector
import org.apache.commons.codec.digest.DigestUtils
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.joor.Reflect
import org.mariadb.jdbc.Driver
import java.sql.SQLException
import org.bukkit.ChatColor as CC

class YukiTexture : JavaPlugin() {
    init {
        Driver() // Prevent minimize
    }

    private val prefix = "${CC.GRAY}[${CC.RED}$name${CC.GRAY}]${CC.RESET}"

    /**
     * Texture pack URL
     */
    lateinit var tex: String

    /**
     * SHA-1 hash
     * null means undefined and the resource pack needs to be downloaded before sending request to client.
     */
    private var sha1: String? = null

    var db: DBConnector? = null
    var dbPrefix = "server_"

    private var config: YamlConfiguration? = null
    var serverName: String = "server"

    fun getTextureConfig(reload: Boolean = false): YamlConfiguration {
        if (reload || config == null) {
            val file = dataFolder.resolve("texture.yml")
            if (!file.isFile) saveResource(file.name, true)
            config = YamlConfiguration.loadConfiguration(file)
            return config!!
        }
        return config!!
    }

    fun reloadTex(sender: CommandSender? = null) {
        Reflect.on(server)
            .call("getServer")
            .call("setResourcePack", "", "")
        logger.info("デフォルトのリソースパックを無効化しました。")

        val yaml = getTextureConfig(true)
        tex = yaml.getString("url")
        if (tex.isNotBlank()) logger.info("リソースパックのURLを $tex に設定しました。")

        // reset sha1 hash so we can re-download the resource pack and calculate the sha1 hash again
        sha1 = null

        sender?.sendMessage("$prefix ${CC.GREEN}リソースパックのURLを再読み込みしました。")
    }

    fun applyTex(player: Player) {
        if (tex.isBlank()) return

        // update sha1 hash of resource pack only if sha1 hash is not calculated yet
        if (sha1 === null) {
            val (_, response, result) = FuelManager()
                .addRequestInterceptor { next: (Request) -> Request ->
                    { req: Request ->
                        JSONMessage.actionbar("${req.url.host} に接続中...", player)
                        next(req)
                    }
                }
                .get(tex)
                .responseProgress { readBytes, totalBytes ->
                    val percent = readBytes.toFloat().div(totalBytes).times(100)
                    JSONMessage.actionbar("リソースパックをダウンロード中... ($percent %)", player)
                }
                .response()
            JSONMessage.create()
                .then("$prefix レスポンスは ")
                .then("${response.statusCode} (${response.responseMessage})")
                .tooltip(buildString {
                    append("${CC.YELLOW}URL: ${CC.RESET}${response.url}")
                    append("\n")
                    append(
                        response.headers
                            .entries
                            .joinToString("\n") {
                                "${CC.AQUA}${it.key}: ${CC.RESET}${it.value.joinToString(" ")}"
                            }
                    )
                })
                .then(" です。")
                .send(player)
            if (result is Result.Failure) {
                result.getException().printStackTrace()
                return
            }
            sha1 = DigestUtils.sha1Hex(result.get())
        }
        JSONMessage.create()
            .title(0, 100, 20, player)
        JSONMessage.create("プレイヤーのリソースパックを変更中...")
            .subtitle(player)
        player.setResourcePack(tex, sha1)
        JSONMessage.create()
            .then("$prefix ")
            .then("${CC.GREEN}完了しました。")
            .tooltip("SHA-1: $sha1")
            .send(player)
    }

    override fun onEnable() {
        reloadTex()
        DBConnector // load driver
        val yaml = getTextureConfig()
        serverName = yaml.getString("server")
        val host = yaml.getString("database.host", "localhost")
        val name = yaml.getString("database.name", "yukitexture")
        val user = yaml.getString("database.user", "yukitexture")
        val password = yaml.getString("database.password")
        dbPrefix = yaml.getString("database.prefix", "server_")
        if (host.isNullOrEmpty() || name.isNullOrEmpty() || user.isNullOrEmpty() || password.isNullOrEmpty()) {
            db = null
            logger.warning("1つ以上のデータベース設定が空です。データベースなしで続行します。")
        } else {
            db = DBConnector(host, name, user, password)
            logger.info("データベースに接続中...")
            try {
                db?.connect()
                db?.execute("""
                    CREATE TABLE IF NOT EXISTS `${dbPrefix}players` (
                      `uuid` varchar(36) NOT NULL,
                      `last_server` varchar(255) DEFAULT NULL,
                      `pending_quit` tinyint(1) NOT NULL DEFAULT 0,
                      PRIMARY KEY (`uuid`)
                    );
                """.trimIndent())
                logger.info("データベースに接続しました。")
            } catch (e: SQLException) {
                logger.warning("データベースに接続できませんでした。データベースなしで続行します。")
                e.printStackTrace()
            }
        }

        getCommand("tex").executor = TextureCommand(this)
        getCommand("reloadtex").executor = ReloadTextureCommand(this)
        server.pluginManager.registerEvents(TextureListener(this), this)
    }

    override fun onDisable() {
        server.messenger.unregisterOutgoingPluginChannel(this)
        server.messenger.unregisterIncomingPluginChannel(this)
        db?.execute("UPDATE `${dbPrefix}players` SET `last_server` = NULL, `pending_quit` = 0 WHERE `last_server` = ?", serverName)
    }
}
