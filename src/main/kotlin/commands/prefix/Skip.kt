package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.service.getOrCreateMusicManager
import dev.pierrot.service.tempReply
import net.dv8tion.jda.api.Permission

class Skip : BasePrefixCommand() {
    override val name: String = "skip"
    override val description: String = "bỏ qua bài phát hiện t"
    override val aliases: Array<String> = arrayOf("s")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withBotPermissions(listOf(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
            .withCategory("music")
            .withRequireVoiceChannel()
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val musicManager = getOrCreateMusicManager(guildId = context.event.guild.id, metadata = context.event.channel)

        if (musicManager.isPlaying()) {
            context.event.message.reply("Bỏ qua bài phát hiện tại!").queue()
            musicManager.skip()
            return CommandResult.Success
        }

        tempReply(context.event.message, "❌ | Không có bài nào đang phát")
        return CommandResult.Success
    }

}