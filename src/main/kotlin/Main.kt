package dev.pierrot

import dev.pierrot.database.RootDatabase
import dev.pierrot.listeners.AnimalSync
import kotlin.concurrent.thread

data class Config(
    val app: AppConfig = AppConfig(),
    val appEnv: AppEnvironment = AppEnvironment()
) {
    data class AppConfig(
        val prefix: String = System.getenv("PREFIX") ?: "ish",
        val token: String = System.getenv("TOKEN") ?: "",
        val lavaLinkUrl: String = System.getenv("LAVALINK") ?: "http://localhost:8080",
        val websocket: String = System.getenv("WEBSOCKET") ?: "",
        val clientId: Int = System.getenv("CLIENT_ID")?.toIntOrNull() ?: 0
    )

    data class AppEnvironment(
        val databaseType: String = System.getenv("DATABASE_TYPE") ?: "postgresql",
        val databaseHost: String = System.getenv("DATABASE_HOST") ?: "localhost",
        val databasePort: String = System.getenv("DATABASE_PORT") ?: "5432",
        val databaseName: String = System.getenv("DATABASE_NAME") ?: "postgres",
        val databaseUsername: String = System.getenv("DATABASE_USERNAME") ?: "postgres",
        val databasePassword: String = System.getenv("DATABASE_PASSWORD") ?: "postgres",
    )
}

lateinit var config: Config

fun main() {

    Runtime.getRuntime().addShutdownHook(thread(start = false, block = RootDatabase::disconnectDatabase))

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
