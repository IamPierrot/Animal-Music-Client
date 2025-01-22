package dev.pierrot.database.schemas

import dev.pierrot.database.entities.Prefix
import org.ktorm.schema.Table
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object Prefixes : Table<Prefix>("Prefix") {
    val guildId = text("guildId").primaryKey().references(Guilds) { it.guild }
    val prefix = varchar("prefix").bindTo { it.prefixName }
}