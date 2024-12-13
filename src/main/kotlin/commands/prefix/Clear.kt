package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.service.getOrCreateMusicManager
import java.time.Duration

class Clear : BasePrefixCommand() {
    override val name: String = "clear"
    override val description: String = "dọn dẹp hàng chờ"
    override val aliases: Array<String> = arrayOf("cls")
    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("music")
            .withCooldown(Duration.ofSeconds(10))
            .withRequireVoiceChannel()
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val musicManager = getOrCreateMusicManager(context.event.guild.id)

        musicManager.stop()
        context.event.message.reply("Đã dọn sách hàng chờ :3").queue()
        return CommandResult.Success
    }
}