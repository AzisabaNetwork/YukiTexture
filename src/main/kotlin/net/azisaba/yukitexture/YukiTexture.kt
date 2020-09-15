package net.azisaba.yukitexture

import com.github.kittinunf.fuel.httpGet
import me.rayzr522.jsonmessage.JSONMessage
import net.azisaba.yukitexture.command.ReloadTextureCommand
import net.azisaba.yukitexture.command.TextureCommand
import net.azisaba.yukitexture.listener.TextureListener
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.joor.Reflect
import java.io.File
import java.net.URL
import java.security.MessageDigest
import org.bukkit.ChatColor as CC

class YukiTexture : JavaPlugin() {

    private val prefix = "${CC.GRAY}[${CC.RED}$name${CC.GRAY}]${CC.RESET}"

    private var tex = ""

    private fun removeDefaultTex() {
        Reflect.on(server).call("getServer").call("setResourcePack", "", "")
        logger.info("デフォルトのリソースパックを無効化しました。")
    }

    private fun loadTex() {
        saveResource("texture.yml", false)
        tex = YamlConfiguration.loadConfiguration(File(dataFolder, "texture.yml")).getString("url")
        tex.takeIf { it.isNotBlank() }?.run { logger.info("リソースパックのURLを $this に設定しました。") }
    }

    fun reloadTex(sender: CommandSender? = null) {
        removeDefaultTex()
        loadTex()
        sender?.run { sendMessage("$prefix ${CC.GREEN}リソースパックを再読み込みしました。") }
    }

    fun applyTexAsync(player: Player) {
        server.scheduler.runTaskAsynchronously(this) {
            if (tex.isBlank()) {
                player.sendMessage("$prefix ${CC.RED}URLが見つかりません。")
                return@runTaskAsynchronously
            }

            val url = URL(tex)

            JSONMessage.create()
                .then("$prefix ").then(url.host).tooltip(url.toString()).then(" に接続しています...")
                .send(player)

            val (_, response, _) = tex.httpGet().responseString()
            val lastUrl = response.url

            JSONMessage.create()
                .then("$prefix ").then(lastUrl.host).tooltip(lastUrl.toString()).then(" が見つかりました！")
                .send(player)

            val sha1 = MessageDigest.getInstance("SHA-1")
                .digest(response.data)
                .joinToString("") { "%02x".format(it) }

            JSONMessage.create()
                .then("$prefix ").then("SHA-1").tooltip(sha1).then(" を取得しました。")
                .send(player)

            player.sendMessage("$prefix 読み込みをリクエスト中...")
            player.setResourcePack(lastUrl.toString(), sha1)
            player.sendMessage("$prefix ${CC.GREEN}完了しました。")
        }
    }

    override fun onEnable() {
        reloadTex()

        getCommand("tex").executor = TextureCommand(this)
        getCommand("reloadtex").executor = ReloadTextureCommand(this)
        server.pluginManager.registerEvents(TextureListener(this), this)
    }
}
