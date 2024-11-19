package dev.pierrot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import dev.pierrot.listeners.AnimalSync
import java.io.File
import java.io.IOException

data class Config(
    val app: AppConfig = AppConfig()
) {
    class AppConfig {
        lateinit var prefix: String
        lateinit var token: String
        lateinit var port: Number
        lateinit var mongo_uri: String
        lateinit var websocket: String
        lateinit var clientId: Number
    }
}

fun isRunningFromJar(): Boolean {
    val resource = object {}.javaClass.getResource("/${object {}.javaClass.name.replace('.', '/')}.class")
    return resource != null && resource.protocol == "jar"
}

lateinit var config: Config

fun main() {
    val configPath = if (isRunningFromJar()) "config.yml" else "src/main/resources/config.yml"
    val file = File(configPath)
    val objectMapper = ObjectMapper(YAMLFactory())

    try {
        config = objectMapper.readValue(file, Config::class.java)
    } catch (error: IOException) {
        throw RuntimeException(error)
    }

    AnimalSync.initialize(config.app.clientId.toInt())
    App.getInstance()
}
