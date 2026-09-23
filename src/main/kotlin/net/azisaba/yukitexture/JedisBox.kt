package net.azisaba.yukitexture

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.io.Closeable

class JedisBox(
    hostname: String,
    port: Int,
    username: String? = null,
    password: String? = null
) : Closeable {
    val jedisPool: JedisPool = createPool(hostname, port, username, password)

    override fun close() {
        jedisPool.close()
    }

    companion object {
        @JvmStatic
        fun createPool(hostname: String, port: Int, username: String?, password: String?): JedisPool {
            return when {
                username != null && password != null -> JedisPool(hostname, port, username, password)
                password != null -> JedisPool(JedisPoolConfig(), hostname, port, 3000, password)
                username != null -> throw IllegalArgumentException("password must not be null when username is provided")
                else -> JedisPool(JedisPoolConfig(), hostname, port)
            }
        }
    }
}
