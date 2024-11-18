package dev.pierrot.commands.types

import dev.pierrot.commands.core.CommandContext

// Cooldown Strategy (Strategy Pattern)
enum class CooldownScope {
    USER {
        override fun getKey(context: CommandContext) = "user:${context.event.author.id}"
    },
    GUILD {
        override fun getKey(context: CommandContext) = "guild:${context.event.guild.id}"
    },
    GLOBAL {
        override fun getKey(context: CommandContext) = "global"
    };

    abstract fun getKey(context: CommandContext): String
}
