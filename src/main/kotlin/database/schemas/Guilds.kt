package dev.pierrot.database.schemas

import dev.pierrot.database.entities.Guild
import org.ktorm.schema.Table
import org.ktorm.schema.text

object Guilds : Table<Guild>("Guild") {
    val guildId = text("guildId").primaryKey().bindTo { it.guildId }
    val guildName = text("guildName").bindTo { it.guildName }
    val guildOwnerId = text("guildOwner").bindTo { it.guildOwnerId }
}