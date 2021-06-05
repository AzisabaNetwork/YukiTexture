package net.azisaba.yukitexture

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.result.Result
import me.rayzr522.jsonmessage.JSONMessage
import net.azisaba.yukitexture.command.ReloadTextureCommand
import net.azisaba.yukitexture.command.TextureCommand
import net.azisaba.yukitexture.listener.TextureListener
import org.apache.commons.codec.digest.DigestUtils
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.joor.Reflect
import org.bukkit.ChatColor as CC

class YukiTexture : JavaPlugin() {

    private val prefix = "${CC.GRAY}[${CC.RED}$name${CC.GRAY}]${CC.RESET}"

    /**
     * Texture pack URL
     */
    private lateinit var tex: String

    /**
     * SHA-1 hash
     * null means undefined and the resource pack needs to be downloaded before sending request to client.
     */
    private var sha1: String? = null

    fun reloadTex(sender: CommandSender? = null) {
        Reflect.on(server)
            .call("getServer")
            .call("setResourcePack", "", "")
        logger.info("デフォルトのリソースパックを無効化しました。")

        val file = dataFolder.resolve("texture.yml")
        if (!file.isFile) saveResource(file.name, true)
        val yaml = YamlConfiguration.loadConfiguration(file)
        tex = yaml.getString("url")
        if (tex.isNotBlank()) logger.info("リソースパックのURLを $tex に設定しました。")

        // reset sha1 hash so we can re-download the resource pack and calculate the sha1 hash again
        sha1 = null

        sender?.sendMessage("$prefix ${CC.GREEN}リソースパックのURLを再読み込みしました。")
    }

    private fun applyTex(player: Player) {
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
                    @Suppress("SimplifiableCallChain")
                    append(response.headers
                        .map { "${CC.AQUA}${it.key}: ${CC.RESET}${it.value.joinToString(" ")}" }
                        .joinToString("\n"))
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

    fun applyTexAsync(player: Player) {
        server.scheduler.runTaskAsynchronously(this) { applyTex(player) }
    }

    override fun onEnable() {
        reloadTex()

        getCommand("tex").executor = TextureCommand(this)
        getCommand("reloadtex").executor = ReloadTextureCommand(this)
        server.pluginManager.registerEvents(TextureListener(this), this)
    }
}
