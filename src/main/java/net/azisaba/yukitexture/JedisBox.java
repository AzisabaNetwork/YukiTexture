package net.azisaba.yukitexture;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Closeable;
import java.util.Objects;

public final class JedisBox implements Closeable {
    private final JedisPool jedisPool;

    public JedisBox(@NotNull String hostname, int port, @Nullable String username, @Nullable String password) {
        this.jedisPool = createPool(hostname, port, username, password);
    }

    @Contract(pure = true)
    @NotNull
    public JedisPool getJedisPool() {
        return jedisPool;
    }

    @Override
    public void close() {
        getJedisPool().close();
    }

    @Contract("_, _, _, _ -> new")
    public static @NotNull JedisPool createPool(@NotNull String hostname, int port, @Nullable String username, @Nullable String password) {
        Objects.requireNonNull(hostname, "hostname");
        if (username != null && password != null) {
            return new JedisPool(hostname, port, username, password);
        } else if (password != null) {
            return new JedisPool(new JedisPoolConfig(), hostname, port, 3000, password);
        } else if (username != null) {
            throw new IllegalArgumentException("password must not be null when username is provided");
        } else {
            return new JedisPool(new JedisPoolConfig(), hostname, port);
        }
    }
}
