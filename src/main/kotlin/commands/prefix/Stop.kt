package dev.pierrot.commands.prefix

import dev.pierrot.App
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.getOrCreateMusicManager
import net.dv8tion.jda.api.Permission

class Stop : BasePrefixCommand() {
    override val name: String = "stop"
    override val description: String = "dừng hàng phát ngay lập tức"
    override val aliases: Array<String> = arrayOf("dung", "yamate", "cut", "cook", "thuongem")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("music")
            .withBotPermissions(listOf(Permission.VOICE_SPEAK, Permission.VOICE_CONNECT))
            .withRequireVoiceChannel()
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val musicManager = getOrCreateMusicManager(context.event.guild.id)

        context.event.jda.directAudioController.disconnect(context.event.guild)
        musicManager.stop()
        context.event.message.reply("Đã dọn sách hàng chờ và xin chào tạm biệt <3").queue()

        App.ServiceLocator.musicManagerStrategy.removeMusicManager(context.event.guild.id)

        return CommandResult.Success
    }
}