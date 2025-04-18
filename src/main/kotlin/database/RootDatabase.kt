package dev.pierrot.database

import dev.pierrot.config
import dev.pierrot.service.getLogger
import org.ktorm.database.Database
import java.sql.Connection
import java.sql.SQLException
import kotlin.system.exitProcess

object RootDatabase {
    private var _db: Database? = null
    private val logger = getLogger("database")

    var db: Database
        get() {
            if (_db == null || !isDatabaseAvailable()) throw SQLException("Database is not available")
            return _db!!
        }
        private set(value) {
            _db = value
        }

    init {
        connectDatabase()
    }

    private fun connectDatabase() {
        logger.info(resolveDatabaseUrl())
        try {
            db = Database.connect(
                url = resolveDatabaseUrl(),
                driver = "org.${config.appEnv.databaseType}.Driver",
                user = config.appEnv.databaseUsername,
                password = config.appEnv.databasePassword
            )
        } catch (ex: Exception) {
            logger.error("Failed to connect to the database: ${ex.message}", ex)
            exitProcess(1)
        }
    }

    private fun resolveDatabaseUrl(): String {
        return "jdbc:${config.appEnv.databaseType}://${config.appEnv.databaseHost}:${config.appEnv.databasePort}/${config.appEnv.databaseName}"
    }

    private fun isDatabaseAvailable(): Boolean {
        return try {
            _db?.useConnection { conn: Connection ->
                !conn.isClosed && conn.isValid(5)
            } ?: false
        } catch (ex: SQLException) {
            logger.error("Database availability check failed: ${ex.message}", ex)
            false
        }
    }

    fun disconnectDatabase() {
        try {
            _db?.useConnection { it.close() }
            _db = null
            logger.info("Database disconnected successfully.")
        } catch (ex: Exception) {
            logger.error("Failed to disconnect the database: ${ex.message}", ex)
        }
    }
}
