package dev.pierrot.commands.prefix

import dev.pierrot.LoopMode
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.embed
import dev.pierrot.getOrCreateMusicManager
import dev.pierrot.tempReply
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

class Loop : BasePrefixCommand() {
    override val name: String = "loop"
    override val description: String = "Điều chỉnh lặp hàng phát"
    override val aliases: Array<String> = arrayOf("lap")

    override fun executeCommand(context: CommandContext): CommandResult {
        val guildMusicManager = getOrCreateMusicManager(context.event.guild.id, context.event.channel)
        val methods = arrayOf("Lặp bài hát", "Lặp cả hàng chờ", "tắt vòng lặp")
        val loopMode = guildMusicManager.scheduler.getLoopMode()

        if (!guildMusicManager.isPlaying()) {
            tempReply(context.event.message, "❌ | Không có bài nào đang phát")
            return CommandResult.Success
        }


        when (loopMode) {
            0 -> {
                guildMusicManager.scheduler.setLoopMode(LoopMode.TRACK)
            }

            1 -> {
                guildMusicManager.scheduler.setLoopMode(LoopMode.QUEUE)
            }

            2 -> {
                guildMusicManager.scheduler.setLoopMode(LoopMode.NONE)
            }

            else -> {}
        }
        val loopEmbed = embed()
            .setColor(Color.pink)
            .setDescription("Thiết lập chế độ : **${methods[loopMode]}** ✅")

        context.event.message.replyEmbeds(loopEmbed.build()).queue()
        return CommandResult.Success
    }


}