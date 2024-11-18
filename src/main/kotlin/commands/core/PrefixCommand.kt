package dev.pierrot.commands.core

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Duration

// Command Interface (Command Pattern)
interface PrefixCommand {
    val name: String
    val description: String
    val aliases: Array<String>
    fun execute(context: CommandContext): CommandResult
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