package dev.pierrot.database.entities

import org.ktorm.entity.Entity

interface Prefix : Entity<Prefix> {
    companion object : Entity.Factory<Prefix>()

    val guildId: String
    val prefix: String
}