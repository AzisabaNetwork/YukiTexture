package net.azisaba.yukitexture

import net.azisaba.yukitexture.command.ReloadTextureCommand
import net.azisaba.yukitexture.command.TextureCommand
import net.azisaba.yukitexture.listener.TextureListener
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.bukkit.ChatColor as CC

class YukiTexture : JavaPlugin() {
    val prefix = "${CC.GRAY}[${CC.RED}$name${CC.GRAY}]${CC.RESET}"

    /**
     * Texture pack URL
     */
    var tex: String = ""
        private set

    /**
     * SHA-1 hash
     */
    @Volatile
    var sha1: String? = null
        private set

    /**
     * Cache of servers that should not apply texture
     */
    var dontApplyTextureServers: Set<String> = emptySet()
        private set

    var jedisBox: JedisBox? = null

    private var config: YamlConfiguration? = null

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
        val yaml = getTextureConfig(true)
        tex = yaml.getString("url") ?: ""
        val configuredSha1 = yaml.getString("sha1")?.trim()
        dontApplyTextureServers = yaml.getStringList("dontApplyTexture").toSet()

        if (tex.isNotBlank()) {
            logger.info("リソースパックのURLを $tex に設定しました。")
        }

        if (!configuredSha1.isNullOrBlank()) {
            sha1 = configuredSha1
            logger.info("リソースパックのSHA-1ハッシュを設定ファイルから適用しました: $sha1")
            sender?.sendMessage("$prefix ${CC.GREEN}リソースパックの設定を再読み込みしました。")
            return
        }

        sha1 = null
        sender?.sendMessage("$prefix ${CC.GREEN}リソースパックの設定を再読み込みしました。")

        if (tex.isNotBlank()) {
            val targetUrl = tex
            server.scheduler.runTaskAsynchronously(this, Runnable {
                try {
                    logger.info("リソースパックのハッシュを計算中... ($targetUrl)")
                    val calculated = calculateSha1FromUrl(targetUrl)
                    if (tex == targetUrl) {
                        sha1 = calculated
                        logger.info("リソースパックのSHA-1ハッシュの計算が完了しました: $calculated")
                    }
                } catch (e: Exception) {
                    logger.warning("リソースパックのSHA-1ハッシュの計算に失敗しました: ${e.message}")
                }
            })
        }
    }

    fun applyTex(player: Player) {
        if (tex.isBlank()) return

        val hash = sha1 ?: ""
        runOnMainThread {
            if (!player.isOnline) return@runOnMainThread
            player.setResourcePack(tex, hash)
        }
    }

    fun runOnMainThread(action: () -> Unit) {
        if (server.isPrimaryThread) {
            action()
        } else {
            server.scheduler.runTask(this, Runnable { action() })
        }
    }

    override fun onEnable() {
        reloadTex()
        val yaml = getTextureConfig()
        val redis = yaml.getConfigurationSection("redis") ?: error("redis section is missing")
        val host = redis.getString("host", "localhost")!!
        val port = if (redis.contains("port")) redis.getInt("port", 6379) else redis.getInt("config", 6379)
        val user = redis.getString("user")
        val password = redis.getString("password")
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
        jedisBox?.close()
        jedisBox = null
    }

    companion object {
        fun calculateSha1FromUrl(urlString: String): String {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "YukiTexture/Minecraft-Server")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP request failed with status code $responseCode: ${connection.responseMessage}")
            }

            connection.inputStream.use { input ->
                return calculateSha1(input)
            }
        }

        fun calculateSha1(input: InputStream): String {
            val digest = MessageDigest.getInstance("SHA-1")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            val hashBytes = digest.digest()
            val hexString = StringBuilder(hashBytes.size * 2)
            for (b in hashBytes) {
                val hex = Integer.toHexString(b.toInt() and 0xFF)
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            return hexString.toString()
        }
    }
}
