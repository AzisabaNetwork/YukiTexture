package net.azisaba.yukitexture

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.RedisClient
import java.io.Closeable

class JedisBox(
    hostname: String,
    port: Int,
    username: String? = null,
    password: String? = null,
    timeoutMillis: Int = 3000
) : Closeable {
    val client: RedisClient = createClient(hostname, port, username, password, timeoutMillis)

    override fun close() {
        client.close()
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun createClient(
            hostname: String,
            port: Int,
            username: String? = null,
            password: String? = null,
            timeoutMillis: Int = 3000
        ): RedisClient {
            if (username != null && password == null) {
                throw IllegalArgumentException("password must not be null when username is provided")
            }

            val clientConfig: JedisClientConfig = DefaultJedisClientConfig.builder()
                .timeoutMillis(timeoutMillis)
                .apply {
                    if (username != null && password != null) {
                        user(username)
                        password(password)
                    } else if (password != null) {
                        password(password)
                    }
                }
                .build()

            return RedisClient.builder()
                .hostAndPort(hostname, port)
                .clientConfig(clientConfig)
                .build()
        }
    }
}