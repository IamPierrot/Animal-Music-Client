package dev.pierrot.handlers

import dev.arbjerg.lavalink.client.AbstractAudioLoadResultHandler
import dev.arbjerg.lavalink.client.player.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color

class AudioLoader(private val event: MessageReceivedEvent, private val guildMusicManager: GuildMusicManager) : AbstractAudioLoadResultHandler() {
    override fun ontrackLoaded(result: TrackLoaded) {
        val track = result.track

        event.message.replyEmbeds(trackEmbed(track)).queue()

        guildMusicManager.scheduler.enqueue(track)
    }

    override fun onPlaylistLoaded(result: PlaylistLoaded) {
        guildMusicManager.scheduler.enqueuePlaylist(result.tracks)

        event.message.replyEmbeds(playlistEmbed(result.tracks)).queue()
    }

    override fun onSearchResultLoaded(result: SearchResult) {
        val tracks = result.tracks

        if (tracks.isEmpty()) {
            event.guildChannel.sendMessage("No tracks found!").queue()
            return
        }

        val firstTrack = tracks.first()

        event.message.replyEmbeds(trackEmbed(firstTrack)).queue()

        guildMusicManager.scheduler.enqueue(firstTrack)
    }

    override fun noMatches() {
        event.guildChannel.sendMessage("No matches found for your input!").queue()
    }

    override fun loadFailed(result: LoadFailed) {
        event.guildChannel.sendMessage("Failed to load track! " + result.exception.message).queue()
    }

    private fun trackEmbed(track: Track): MessageEmbed {
        val trackInfo = track.info

        return EmbedBuilder()
            .setAuthor("THÊM VÀO HÀNG CHỜ", null, trackInfo.artworkUrl)
            .setDescription("Đã thêm [${trackInfo.title}](${trackInfo.uri}) vào hàng chờ!")
            .setFooter("💖 Âm nhạc đi trước tình yêu theo sau", event.jda.selfUser.avatarUrl)
            .setThumbnail(trackInfo.artworkUrl)
            .setColor(Color.pink).build()
    }

    private fun playlistEmbed(playlist: List<Track>): MessageEmbed {
        val trackInfo = playlist.first().info
        return EmbedBuilder()
            .setAuthor("THÊM PLAYLIST", null, trackInfo.artworkUrl)
            .setDescription("Đã thêm **${playlist.size}** bài hát vào hàng chờ!")
            .setFooter("💖 Âm nhạc đi trước tình yêu theo sau", event.jda.selfUser.avatarUrl)
            .setThumbnail(trackInfo.artworkUrl)
            .setColor(Color.pink).build()
    }

}