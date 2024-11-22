package dev.pierrot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import dev.pierrot.listeners.AnimalSync
import java.io.File
import java.io.IOException

data class Config(
    val app: AppConfig = AppConfig()
) {
    data class AppConfig(
        val prefix: String = "",
        val token: String = "",
        val port: Int = 8080,
        val mongo_uri: String = "",
        val websocket: String = "",
        val clientId: Int = 0
    )
}

fun isRunningFromJar(): Boolean {
    val resource = object {}.javaClass.getResource("/${object {}.javaClass.name.replace('.', '/')}.class")
    return resource?.protocol == "jar"
}

lateinit var config: Config

fun main() {
    System.setProperty("sun.security.util.ManifestFileVerifier", "false")

    val configPath = if (isRunningFromJar()) {
        File("config.yml")
    } else {
        File("src/main/resources/config.yml")
    }

    if (!configPath.exists()) {
        System.err.println("Config file not found: ${configPath.absolutePath}")
        return
    }

    val objectMapper = ObjectMapper(YAMLFactory())

    try {
        config = objectMapper.readValue(configPath, Config::class.java)
        AnimalSync.initialize(config.app.clientId).run {
            App.getInstance()
        }
    } catch (error: IOException) {
        System.err.println("Error reading config file: ${error.message}")
        error.printStackTrace()
    } catch (error: Exception) {
        System.err.println("Application failed to start: ${error.message}")
        error.printStackTrace()
    }
}
