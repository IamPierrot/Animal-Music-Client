package dev.pierrot.commands.core

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.getLogger
import org.reflections.Reflections
import org.slf4j.Logger
import java.time.Duration

// Cooldown Manager (Singleton Pattern)
class CooldownManager {
    private val cooldowns = mutableMapOf<String, Long>()

    fun getRemainingCooldown(key: String): Duration {
        val lastUse = cooldowns[key] ?: return Duration.ZERO
        val timePassed = System.currentTimeMillis() - lastUse
        val remainingTime = Duration.ofSeconds(2).toMillis() - timePassed
        return if (remainingTime > 0) Duration.ofMillis(remainingTime) else Duration.ZERO
    }

    fun applyCooldown(key: String, duration: Duration) {
        if (duration > Duration.ZERO) {
            cooldowns[key] = System.currentTimeMillis()
        }
    }
}

// Command Registry (Singleton Pattern)
object CommandRegistry {
    private val logger: Logger = getLogger(CommandRegistry::class.java)
    private val commands = mutableMapOf<String, PrefixCommand>()
    private val aliases = mutableMapOf<String, String>()

    private fun registerCommand(prefixCommand: PrefixCommand) {
        commands[prefixCommand.name.lowercase()] = prefixCommand
        prefixCommand.aliases.forEach { alias ->
            aliases[alias.lowercase()] = prefixCommand.name.lowercase()
        }
        logger.info("Registered command: ${prefixCommand.name}")
    }

    fun getCommand(name: String): PrefixCommand? {
        val commandName = aliases[name.lowercase()] ?: name.lowercase()
        return commands[commandName]
    }

    fun loadCommands() {
        val reflections = Reflections("dev.pierrot.commands.prefix")
        reflections.getSubTypesOf(BasePrefixCommand::class.java)
            .forEach { commandClass ->
                try {
                    registerCommand(commandClass.getConstructor().newInstance())
                } catch (e: Exception) {
                    logger.error("Failed to register command: ${commandClass.simpleName}", e)
                }
            }
    }
}