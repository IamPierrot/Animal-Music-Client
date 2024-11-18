package dev.pierrot.commands.config

import dev.pierrot.config
import net.dv8tion.jda.api.Permission
import java.time.Duration

// Command Configuration (Builder Pattern)
class CommandConfig private constructor(
    val prefix: String = config.app.prefix,
    val cooldown: Duration = Duration.ofSeconds(2),
    val usage: String = "",
    val deleteCommandMessage: Boolean = false,
    val requireBotPermissions: List<Permission> = emptyList(),
    val requireUserPermissions: List<Permission> = emptyList(),
    val category: String = "Misc"
) {
    class Builder {
        private var prefix: String = config.app.prefix
        private var cooldown: Duration = Duration.ofSeconds(2)
        private var usage: String = ""
        private var deleteCommandMessage: Boolean = false
        private var requireBotPermissions: List<Permission> = emptyList()
        private var requireUserPermissions: List<Permission> = emptyList()
        private var category: String = "Misc"

        fun withPrefix(prefix: String) = apply { this.prefix = prefix }
        fun withCooldown(cooldown: Duration) = apply { this.cooldown = cooldown }
        fun withUsage(usage: String) = apply { this.usage = usage }
        fun withDeleteMessage(delete: Boolean) = apply { this.deleteCommandMessage = delete }
        fun withBotPermissions(permissions: List<Permission>) = apply { this.requireBotPermissions = permissions }
        fun withUserPermission(permissions: List<Permission>) = apply { this.requireUserPermissions = permissions }
        fun withCategory(category: String) = apply { this.category = category }

        fun build() = CommandConfig(
            prefix,
            cooldown,
            usage,
            deleteCommandMessage,
            requireBotPermissions,
            requireUserPermissions,
            category
        )
    }
}