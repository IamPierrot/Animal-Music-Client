package dev.pierrot.database.entities

import org.ktorm.entity.Entity

interface Prefix : Entity<Prefix> {
    companion object : Entity.Factory<Prefix>()

    val guildId: String
    val prefixName: String
    val guild: Guild
}