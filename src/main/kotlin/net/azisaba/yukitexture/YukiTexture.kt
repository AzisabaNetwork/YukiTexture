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
    val prefix: String
        get() = "${CC.GRAY}[${CC.RED}$name${CC.GRAY}]${CC.RESET}"

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
     * Cache of servers that should not apply texture (case-insensitive)
     */
    var dontApplyTextureServers: Set<String> = java.util.TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        private set

    /**
     * Optional version-specific resource packs
     */
    var versionPacks: List<VersionPack> = emptyList()
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

        val servers = java.util.TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        servers.addAll(yaml.getStringList("dontApplyTexture").map { it.trim() }.filter { it.isNotEmpty() })
        dontApplyTextureServers = servers

        val parsedVersionPacks = mutableListOf<VersionPack>()
        val rawVersionPacks = yaml.getMapList("versionPacks")
        for (entry in rawVersionPacks) {
            val packUrl = entry["url"]?.toString()?.trim() ?: continue
            if (packUrl.isEmpty()) continue
            val packSha1 = entry["sha1"]?.toString()?.trim()?.ifBlank { null }

            val exactVersion = entry["version"]
            val minVersion = entry["min"] ?: entry["minVersion"]
            val maxVersion = entry["max"] ?: entry["maxVersion"]

            val minProtocol = if (exactVersion != null) VersionPack.parseProtocol(exactVersion) else VersionPack.parseProtocol(minVersion)
            val maxProtocol = if (exactVersion != null) VersionPack.parseProtocol(exactVersion) else VersionPack.parseProtocol(maxVersion)

            parsedVersionPacks.add(VersionPack(minProtocol, maxProtocol, packUrl, packSha1))
        }
        versionPacks = parsedVersionPacks

        if (tex.isNotBlank()) {
            logger.info("デフォルトリソースパックのURLを $tex に設定しました。")
        }
        if (versionPacks.isNotEmpty()) {
            logger.info("${versionPacks.size} 個のバージョン別リソースパック設定を読み込みました。")
        }

        if (!configuredSha1.isNullOrBlank()) {
            sha1 = configuredSha1
            logger.info("デフォルトリソースパックのSHA-1ハッシュを設定ファイルから適用しました: $sha1")
        } else {
            sha1 = null
        }

        val urlsToFetch = mutableListOf<Pair<String, (String) -> Unit>>()
        if (tex.isNotBlank() && sha1 == null) {
            val targetUrl = tex
            urlsToFetch.add(targetUrl to { calculated ->
                if (tex == targetUrl) {
                    sha1 = calculated
                    logger.info("デフォルトリソースパックのSHA-1ハッシュの計算が完了しました: $calculated")
                }
            })
        }
        for (pack in versionPacks) {
            if (pack.sha1 == null && pack.url.isNotBlank()) {
                val targetUrl = pack.url
                urlsToFetch.add(targetUrl to { calculated ->
                    if (pack.url == targetUrl) {
                        pack.sha1 = calculated
                        logger.info("バージョン別リソースパック (${pack.url}) のSHA-1ハッシュの計算が完了しました: $calculated")
                    }
                })
            }
        }

        if (urlsToFetch.isNotEmpty()) {
            server.scheduler.runTaskAsynchronously(this, Runnable {
                val cache = mutableMapOf<String, String>()
                for ((targetUrl, callback) in urlsToFetch) {
                    try {
                        val calculated = cache.getOrPut(targetUrl) {
                            logger.info("リソースパックのハッシュを計算中... ($targetUrl)")
                            calculateSha1FromUrl(targetUrl)
                        }
                        callback(calculated)
                    } catch (e: Exception) {
                        logger.warning("リソースパック ($targetUrl) のSHA-1ハッシュ計算に失敗しました: ${e.message}")
                    }
                }
            })
        }

        sender?.sendMessage("$prefix ${CC.GREEN}リソースパックの設定を再読み込みしました。")
    }

    fun applyTex(player: Player, cachedProtocolVersion: Int? = null) {
        val (packUrl, packHash) = selectPackForPlayer(player, cachedProtocolVersion)
        if (packUrl.isBlank()) return

        runOnMainThread {
            if (!player.isOnline) return@runOnMainThread
            player.setResourcePack(packUrl, packHash ?: "")
        }
    }

    private fun selectPackForPlayer(player: Player, cachedProtocolVersion: Int? = null): Pair<String, String?> {
        if (versionPacks.isNotEmpty()) {
            val protocolVersion = cachedProtocolVersion ?: getPlayerProtocolVersion(player)
            if (protocolVersion != null) {
                val matched = versionPacks.firstOrNull { it.matches(protocolVersion) }
                if (matched != null) {
                    return matched.url to (matched.sha1 ?: "")
                }
            }
        }
        return tex to (sha1 ?: "")
    }

    private fun getPlayerProtocolVersion(player: Player): Int? {
        try {
            val data = PlayerData.getByUUID(player.uniqueId)
            if (data?.protocolVersion != null) {
                return data.protocolVersion
            }
        } catch (_: Throwable) {
        }
        if (isViaVersionAvailable()) {
            try {
                return getProtocolVersionFromViaVersion(player.uniqueId)
            } catch (e: Throwable) {
                logger.warning("ViaVersion からのプレイヤーバージョン取得に失敗しました: ${e.message}")
            }
        }

        return null
    }

    private fun isViaVersionAvailable(): Boolean {
        return server.pluginManager.isPluginEnabled("ViaVersion")
    }

    private fun getProtocolVersionFromViaVersion(uuid: java.util.UUID): Int? {
        return try {
            val viaClass = Class.forName("com.viaversion.viaversion.api.Via")
            val getApiMethod = viaClass.getMethod("getAPI")
            val api = getApiMethod.invoke(null)
            val getPlayerVersionMethod = api.javaClass.getMethod("getPlayerVersion", java.util.UUID::class.java)
            getPlayerVersionMethod.invoke(api, uuid) as? Int
        } catch (_: Throwable) {
            null
        }
    }

    fun runOnMainThread(action: () -> Unit) {
        if (server.isPrimaryThread) {
            action()
        } else {
            server.scheduler.runTask(this, Runnable { action() })
        }
    }

    fun addDontApplyServer(serverName: String): Boolean {
        val trimmed = serverName.trim()
        if (trimmed.isEmpty() || dontApplyTextureServers.contains(trimmed)) return false
        val newSet = java.util.TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        newSet.addAll(dontApplyTextureServers)
        newSet.add(trimmed)
        dontApplyTextureServers = newSet
        saveDontApplyServersToConfig()
        return true
    }

    fun removeDontApplyServer(serverName: String): Boolean {
        val trimmed = serverName.trim()
        if (!dontApplyTextureServers.contains(trimmed)) return false
        val newSet = java.util.TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        newSet.addAll(dontApplyTextureServers)
        newSet.remove(trimmed)
        dontApplyTextureServers = newSet
        saveDontApplyServersToConfig()
        return true
    }

    private fun saveDontApplyServersToConfig() {
        val file = dataFolder.resolve("texture.yml")
        val yaml = getTextureConfig()
        yaml.set("dontApplyTexture", dontApplyTextureServers.toList())
        yaml.save(file)
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
            jedisBox?.client?.ping()
            logger.info("Redisに接続しました。")
        } catch (e: Exception) {
            logger.warning("Redisに接続できませんでした。データベースなしで続行します。")
            e.printStackTrace()
            jedisBox = null
        }

        val textureCommand = TextureCommand(this)
        getCommand("tex")?.setExecutor(textureCommand)
        getCommand("tex")?.tabCompleter = textureCommand
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
