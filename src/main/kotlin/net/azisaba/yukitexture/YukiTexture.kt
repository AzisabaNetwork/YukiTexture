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

    private lateinit var tex: String

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

        sender?.sendMessage("$prefix ${CC.GREEN}リソースパックのURLを再読み込みしました。")
    }

    private fun applyTex(player: Player) {
        if (tex.isBlank()) return

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
        val sha1 = DigestUtils.sha1Hex(result.get())
        JSONMessage.create()
            .then("$prefix ")
            .then("SHA-1")
            .tooltip(sha1)
            .then(" を計算しました。")
            .send(player)
        JSONMessage.create()
            .title(0, 100, 20, player)
        JSONMessage.create("プレイヤーのリソースパックを変更中...")
            .subtitle(player)
        player.setResourcePack(response.url.toString(), sha1)
        player.sendMessage("$prefix ${CC.GREEN}完了しました。")
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
