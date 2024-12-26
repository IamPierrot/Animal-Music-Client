package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.config
import dev.pierrot.service.getOrCreateMusicManager
import dev.pierrot.service.tempReply

class Volume : BasePrefixCommand() {
    override val name: String = "volume"
    override val description: String = "Điều chỉnh âm thanh"
    override val aliases: Array<String> = arrayOf("vol", "v")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("music")
            .withRequireVoiceChannel()
            .withUsage("${config.app.prefix} $name <số> ")
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val guild = context.event.guild
        val guildMusicManager = getOrCreateMusicManager(guild.id)

        val adjustVolume = context.args[0].toDoubleOrNull()

        if (adjustVolume == null || adjustVolume.isNaN() || adjustVolume >= 100 || adjustVolume <= 0)
            return CommandResult.InvalidArguments

        if (!guildMusicManager.isPlaying()) {
            tempReply(context.event.message, "❌ | Không có bài nào đang phát")
            return CommandResult.Success
        }

        val player = guildMusicManager.getPlayer().orElse(null) ?: return CommandResult.Error("Unable to find player!")
        player.setVolume(adjustVolume.toInt())

        context.event.message.reply("⚡ | Điều chỉnh âm thanh phát nhạc là $adjustVolume%!")
        return CommandResult.Success
    }
}