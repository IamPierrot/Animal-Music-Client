package dev.pierrot.commands.prefix

import dev.arbjerg.lavalink.client.player.Track
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.embed
import dev.pierrot.getOrCreateMusicManager
import java.util.*
import java.util.stream.Collectors
import kotlin.math.min

class History : BasePrefixCommand() {

    override val name: String = "history"
    override val description: String = "Điều chỉnh lặp hàng phát"
    override val aliases: Array<String> = arrayOf("ls", "lichsu")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("music")
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val guild = context.event.guild
        val guildMusicManager = getOrCreateMusicManager(guild.id)

        val histories = guildMusicManager.scheduler.history

        val songCount: Int = histories.size
        val previousSong =
            if (songCount > 15) "Và **${songCount - 15}** bài khác nữa..." else "trong lịch sử là **$songCount** bài hát..."

        val tracks = formatTracks(histories)

        val embed = embed()
            .setAuthor(
                "Danh sách lịch sử phát - ${guild.name}",
                null,
                context.event.jda.selfUser.avatarUrl
            )
            .setDescription(
                tracks.subList(
                    0,
                    min(songCount, 15)
                ).joinToString("\n").plus("\n").plus(previousSong)
            )
            .setFooter("💖 Âm nhạc đi trước tình yêu theo sau", context.event.jda.selfUser.avatarUrl)

        context.event.message.replyEmbeds(embed.build()).queue()
        return CommandResult.Success
    }

    private fun formatTracks(history: Deque<Track>): List<String> {
        val index = intArrayOf(1)
        return history.stream()
            .map { track: Track -> "**${index[0]++}** - ${formatTrack(track)}" }
            .collect(Collectors.toList())
    }

    private fun formatTrack(track: Track): String {
        return "`${track.info.title} | ${track.info.author}`"
    }
}