package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult

class Ping : BasePrefixCommand() {
    override val name = "ping"
    override val description = "Kiểm tra độ trễ"
    override val aliases = emptyArray<String>()
    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("Info")
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        context.event.message.reply("Pong!").queue()
        return CommandResult.Success
    }
}