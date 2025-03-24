package net.azisaba.yukitexture.config

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.encodeToString
import java.io.File

object ConfigUtil {
    fun <T> loadConfig(
        deserializer: DeserializationStrategy<T>,
        configFile: File,
    ): T = Yaml.default.decodeFromString(deserializer, configFile.readText())

    inline fun <reified T> saveConfig(
        configData: T,
        configFile: File,
    ) {
        configFile.writeText(Yaml.default.encodeToString(configData))
    }
}
