package dev.pierrot

import dev.pierrot.database.RootDatabase
import dev.pierrot.listeners.AnimalSync

data class Config(
    val app: AppConfig = AppConfig()
) {
    data class AppConfig(
        val prefix: String = System.getenv("PREFIX") ?: "ish",
        val token: String = System.getenv("TOKEN") ?: throw RuntimeException("No Token supplied"),
        val lavaLinkUrl: String = System.getenv("LAVALINK") ?: "http://localhost:8080",
        val websocket: String = System.getenv("WEBSOCKET") ?: "",
        val clientId: Int = System.getenv("CLIENT_ID")?.toIntOrNull() ?: 0
    )
}

lateinit var config: Config

fun main() {
    System.setProperty("sun.security.util.ManifestFileVerifier", "false")

    try {
        config = Config()
        RootDatabase.apply {
            AnimalSync.initialize(config.app.clientId).run {
                App.getInstance()
            }
        }
    } catch (error: Exception) {
        System.err.println("Application failed to start: ${error.message}")
        error.printStackTrace()
    }
}
