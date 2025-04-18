package dev.pierrot.database.schemas

import dev.pierrot.database.entities.Guild
import org.ktorm.schema.Table
import org.ktorm.schema.text

object Guilds : Table<Guild>("guild") {
    val guildId = text("guild_id").primaryKey().bindTo { it.guildId }
    val guildName = text("guild_name").bindTo { it.guildName }
    val guildOwnerId = text("guild_owner_id").bindTo { it.guildOwnerId }
}