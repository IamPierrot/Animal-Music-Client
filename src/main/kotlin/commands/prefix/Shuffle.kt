package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.getOrCreateMusicManager
import dev.pierrot.tempReply
import java.time.Duration

class Shuffle : BasePrefixCommand() {
    override val name: String = "shuffle"
    override val description: String = "xáo trộn hàng chờ"
    override val aliases: Array<String> = arrayOf("tron", "taixiu")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("music")
            .withRequireVoiceChannel()
            .withCooldown(Duration.ofSeconds(15))
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val guildMusicManager = getOrCreateMusicManager(context.event.guild.id, context.event.channel)

        if (!guildMusicManager.isPlaying()) {
            tempReply(context.event.message, "❌ | Không có bài nào đang phát")
            return CommandResult.Success
        }

        if (guildMusicManager.scheduler.queue.isEmpty()) {
            tempReply(context.event.message, "❌ | Không có gì trong hàng chờ phát")
            return CommandResult.Success
        }

        val shuffledQueue = guildMusicManager.scheduler.queue.shuffled()
        guildMusicManager.scheduler.queue.clear()
        guildMusicManager.scheduler.queue.addAll(shuffledQueue)

        context.event.message.reply("✔️ | Đã xáo trộn thành công **`${guildMusicManager.scheduler.queue.size}`** bài trong hàng chờ").queue()

        return CommandResult.Success

    }
}