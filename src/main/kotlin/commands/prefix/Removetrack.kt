package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.config
import dev.pierrot.service.getOrCreateMusicManager
import java.time.Duration

class Removetrack : BasePrefixCommand() {
    override val name: String = "removetrack"
    override val description: String = "xoá hàng chờ"
    override val aliases: Array<String> = arrayOf("rm")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCooldown(Duration.ofSeconds(5))
            .withUsage("${config.app.prefix} $name <số #= 0>")
            .withCategory("music")
            .withRequireVoiceChannel()
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val index: Int = context.args.first().toInt()
        if (index <= 0) return CommandResult.InvalidArguments

        val guildMusicManager = getOrCreateMusicManager(context.event.guild.id)
        guildMusicManager.scheduler.removeTrack(index - 1)

        context.event.message.reply("Đã xoá thành công bài phát tại hàng chờ $index!")
        return CommandResult.Success
    }

}