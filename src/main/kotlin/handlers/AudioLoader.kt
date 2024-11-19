package dev.pierrot.handlers

import dev.arbjerg.lavalink.client.AbstractAudioLoadResultHandler
import dev.arbjerg.lavalink.client.player.*
import dev.pierrot.App
import dev.pierrot.embed
import dev.pierrot.listeners.AnimalSync
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color

class AudioLoader(private val guildMusicManager: GuildMusicManager, private val voiceChannelId: String) :
    AbstractAudioLoadResultHandler() {
    override fun ontrackLoaded(result: TrackLoaded) {
        val track = result.track

        syncPlayer(listOf(track.info.title))

        guildMusicManager.metadata?.sendMessageEmbeds(trackEmbed(track))?.queue()

        guildMusicManager.scheduler.enqueue(track)
    }

    override fun onPlaylistLoaded(result: PlaylistLoaded) {
        guildMusicManager.scheduler.enqueuePlaylist(result.tracks)
        syncPlayer(result.tracks.map { it.info.title })

        guildMusicManager.metadata?.sendMessageEmbeds(playlistEmbed(result.tracks))?.queue()
    }

    override fun onSearchResultLoaded(result: SearchResult) {
        val tracks = result.tracks

        if (tracks.isEmpty()) {
            guildMusicManager.metadata?.sendMessage("No tracks found!")?.queue()
            return
        }

        val firstTrack = tracks.first()
        syncPlayer(listOf(firstTrack.info.title))

        guildMusicManager.metadata?.sendMessageEmbeds(trackEmbed(firstTrack))?.queue()

        guildMusicManager.scheduler.enqueue(firstTrack)
    }

    override fun noMatches() {
        guildMusicManager.metadata?.sendMessage("No matches found for your input!")?.queue()
    }

    override fun loadFailed(result: LoadFailed) {
        guildMusicManager.metadata?.sendMessage("Failed to load track! " + result.exception.message)?.queue()
    }

    private fun trackEmbed(track: Track): MessageEmbed {
        val trackInfo = track.info

        return embed()
            .setAuthor("THÊM VÀO HÀNG CHỜ", null, trackInfo.artworkUrl)
            .setDescription("Đã thêm [${trackInfo.title}](${trackInfo.uri}) vào hàng chờ!")
            .setFooter("💖 Âm nhạc đi trước tình yêu theo sau", App.ServiceLocator.jda.selfUser.avatarUrl)
            .setThumbnail(trackInfo.artworkUrl)
            .setColor(Color.pink).build()
    }

    private fun playlistEmbed(playlist: List<Track>): MessageEmbed {
        val trackInfo = playlist.first().info
        return embed()
            .setAuthor("THÊM PLAYLIST", null, trackInfo.artworkUrl)
            .setDescription("Đã thêm **${playlist.size}** bài hát vào hàng chờ!")
            .setFooter("💖 Âm nhạc đi trước tình yêu theo sau", App.ServiceLocator.jda.selfUser.avatarUrl)
            .setThumbnail(trackInfo.artworkUrl)
            .setColor(Color.pink).build()
    }

    private fun syncPlayer(tracks: List<String>): Unit {
        guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id?.let {
            AnimalSync.getInstance().send(
                "player_sync",
                AnimalSync.getInstance().clientId.toString(),
                voiceChannelId,
                it,
                tracks
            )
        }
    }

}