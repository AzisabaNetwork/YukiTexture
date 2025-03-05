package net.azisaba.yukitexture

import java.io.Closeable
import java.util.Objects
import org.jetbrains.annotations.Contract
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class JedisBox(hostname: String, port: Int, username: String?, password: String?) : Closeable {
    internal val jedisPool: JedisPool = createPool(hostname, port, username, password)

    @Contract(pure = true)
    fun getJedisPool(): JedisPool {
        return jedisPool
    }

    override fun close() {
        getJedisPool().close()
    }

    companion object {
        @Contract("_, _, _, _ -> new")
        fun createPool(hostname: String, port: Int, username: String?, password: String?): JedisPool {
            Objects.requireNonNull<String?>(hostname, "hostname")
            if (username != null && password != null) {
                return JedisPool(hostname, port, username, password)
            } else if (password != null) {
                return JedisPool(JedisPoolConfig(), hostname, port, 3000, password)
            } else require(username == null) { "password must not be null when username is provided" }
            return JedisPool(JedisPoolConfig(), hostname, port)
        }
    }
}