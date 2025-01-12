package dev.pierrot.database

import dev.pierrot.service.getLogger
import org.ktorm.database.Database
import java.sql.Connection
import java.sql.SQLException
import kotlin.system.exitProcess

object RootDatabase {
    private var _db: Database? = null

    var db: Database
        get() {
            if (_db == null || !isDatabaseAvailable()) throw SQLException("Database is not available")
            return _db!!
        }
        private set(value) {
            _db = value
        }

    private val DATABASE_URL = System.getenv("DATABASE_URL")
        ?: throw IllegalArgumentException("DATABASE_URL is not set")
    private val logger = getLogger("Database")

    init {
        connectDatabase()
    }

    private fun connectDatabase() {
        try {
            db = Database.connect(DATABASE_URL)
            logger.info("Database connected successfully.")
        } catch (ex: Exception) {
            logger.error("Failed to connect to the database: ${ex.message}", ex)
            exitProcess(1)
        }
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
