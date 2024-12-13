package dev.pierrot.commands.prefix

import dev.pierrot.App
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.service.getOrCreateMusicManager
import dev.pierrot.service.tempReply

class Pause : BasePrefixCommand() {
    override val name: String = "pause"
    override val description: String = "tạm dừng/tiếp tục bài phát hiện tại"
    override val aliases: Array<String> = arrayOf("wait")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("music")
            .withRequireVoiceChannel()
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val guildMusicManager = getOrCreateMusicManager(context.event.guild.id, context.event.channel)

        if (!guildMusicManager.isPlaying()) {
            tempReply(context.event.message, "❌ | Không có bài nào đang phát")
            return CommandResult.Success
        }
        App.ServiceLocator.lavalinkClient.getOrCreateLink(context.event.guild.idLong)
            .getPlayer()
            .flatMap { it.setPaused(!it.paused) }
            .subscribe { player ->
                context.event.message.reply("Player has been ${if (player.paused) "paused" else "resumed"} !").queue()
            }

        return CommandResult.Success

    }
}