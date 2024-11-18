package dev.pierrot.commands.core

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.getLogger
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.reflections.Reflections
import org.slf4j.Logger
import java.time.Duration

// Command Interface (Command Pattern)
interface PrefixCommand {
    val name: String
    val description: String
    val aliases: Array<String>
    fun execute(context: CommandContext): CommandResult
    val commandConfig: CommandConfig
}

// Command Results
sealed class CommandResult {
    data object Success : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class CooldownActive(val remainingTime: Duration) : CommandResult()
    data object InsufficientPermissions : CommandResult()
    data object InvalidArguments : CommandResult()
}



// Command Context
data class CommandContext(
    val event: MessageReceivedEvent,
    val args: List<String>,
    val rawArgs: String,
    val prefix: String,
    val isMentionPrefix: Boolean
)

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