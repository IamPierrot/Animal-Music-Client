package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.service.embed
import dev.pierrot.service.getOrCreateMusicManager
import dev.pierrot.service.tempReply
import java.awt.Color
import java.time.Duration

class Loop : BasePrefixCommand() {
    override val name: String = "loop"
    override val description: String = "Điều chỉnh lặp hàng phát"
    override val aliases: Array<String> = arrayOf("lap")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withRequireVoiceChannel()
            .withCooldown(Duration.ofSeconds(5))
            .withCategory("music")
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val guildMusicManager = getOrCreateMusicManager(context.event.guild.id, context.event.channel)
        val methods = arrayOf("Lặp bài hát", "Lặp cả hàng chờ", "tắt vòng lặp")
        val loopMode = guildMusicManager.scheduler.getLoopMode()

        if (!guildMusicManager.isPlaying()) {
            tempReply(context.event.message, "❌ | Không có bài nào đang phát")
            return CommandResult.Success
        }

        guildMusicManager.scheduler.changeLoopMode(loopMode)

        val loopEmbed = embed()
            .setColor(Color.pink)
            .setDescription("Thiết lập chế độ : **${methods[loopMode]}** ✅")

        context.event.message.replyEmbeds(loopEmbed.build()).queue()
        return CommandResult.Success
    }


}