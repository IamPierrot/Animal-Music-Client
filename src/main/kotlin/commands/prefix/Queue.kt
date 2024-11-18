package dev.pierrot.commands.prefix

import dev.arbjerg.lavalink.client.player.Track
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.embed
import dev.pierrot.getOrCreateMusicManager
import dev.pierrot.tempReply
import java.util.Queue
import java.util.stream.Collectors
import kotlin.math.min

class Queue : BasePrefixCommand() {
    override val name: String = "queue"
    override val description: String = "xem hàng chờ hiện tại"
    override val aliases: Array<String> = arrayOf("q")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("music")
            .build()

    private val methods = arrayOf("", "🔁", "🔂")


    override fun executeCommand(context: CommandContext): CommandResult {
        val guild = context.event.guild
        val guildMusicManager = getOrCreateMusicManager(guild.id)


        if (!guildMusicManager.isPlaying()) {
            tempReply(context.event.message, "❌ | Không có bài nào đang phát")
            return CommandResult.Success
        }

        val queue = guildMusicManager.scheduler.queue

        val songCount: Int = queue.size
        val nextSongs =
            if (songCount > 5) "Và **${songCount - 5}** bài khác nữa..." else "Đang trong hàng chờ được phát là **$songCount** bài hát..."

        val tracks = formatTracks(queue)
        val currentTrack = guildMusicManager.getCurrentTrack()!!

        val embed = embed()
            .setAuthor(
                "Danh sách hàng chờ - ${guild.name}",
                null,
                context.event.jda.selfUser.avatarUrl
            )
            .setThumbnail(guild.iconUrl)
            .setDescription(
                "Đang phát **[${currentTrack.info.title}](${currentTrack.info.uri})** ${methods[guildMusicManager.scheduler.getLoopMode()]} \n\n${
                    tracks.subList(
                        0,
                        min(songCount, 5)
                    ).joinToString("\n")
                }\n\n${nextSongs}"
            )
            .setFooter("💖 Âm nhạc đi trước tình yêu theo sau", context.event.jda.selfUser.avatarUrl)

        context.event.message.replyEmbeds(embed.build()).queue()
        return CommandResult.Success
    }

    private fun formatTracks(queue: Queue<Track>): List<String> {
        val index = intArrayOf(1)
        return queue.stream()
            .map { track: Track -> "**${index[0]++}** - ${formatTrack(track)}" }
            .collect(Collectors.toList())
    }

    private fun formatTrack(track: Track): String {
        return "`${track.info.title} | ${track.info.author}`"
    }
}