package dev.pierrot.database.entities

import org.ktorm.entity.Entity

interface Guild : Entity<Guild> {
    companion object : Entity.Factory<Guild>()

    val guildId: String
    val guildName: String
    val guildOwnerId: String
}