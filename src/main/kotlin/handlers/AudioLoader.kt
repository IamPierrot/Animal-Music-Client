package dev.pierrot.handlers

import dev.arbjerg.lavalink.client.AbstractAudioLoadResultHandler
import dev.arbjerg.lavalink.client.player.*
import dev.pierrot.App
import dev.pierrot.embed
import dev.pierrot.listeners.AnimalSync
import dev.pierrot.models.PlayerEvent
import dev.pierrot.models.PlayerSyncData
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color

class AudioLoader(
    private val event: MessageReceivedEvent,
    private val guildMusicManager: GuildMusicManager,
    private val voiceChannelId: String
) :
    AbstractAudioLoadResultHandler() {

    override fun ontrackLoaded(result: TrackLoaded) {
        val track = result.track
        val syncData = PlayerSyncData().apply {
            eventExtend = "event"
            guildId = guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id ?: ""
            voiceChannelId = this@AudioLoader.voiceChannelId
            musicList = listOf(track.info.title)
            event = PlayerEvent().apply {
                type = "TrackLoaded"
                guildId = guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id ?: ""
                channelId = this@AudioLoader.voiceChannelId
            }
        }

        sendPlayerSync(syncData)

        event.message.replyEmbeds(trackEmbed(track)).queue()
        guildMusicManager.scheduler.enqueue(track)
    }

    override fun onPlaylistLoaded(result: PlaylistLoaded) {
        val syncData = PlayerSyncData().apply {
            eventExtend = "event"
            guildId = guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id ?: ""
            voiceChannelId = this@AudioLoader.voiceChannelId
            musicList = result.tracks.map { it.info.title }
            event = PlayerEvent().apply {
                type = "PlaylistLoaded"
                guildId = guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id ?: ""
                channelId = this@AudioLoader.voiceChannelId
            }
        }
        sendPlayerSync(syncData)

        guildMusicManager.scheduler.enqueuePlaylist(result.tracks)
        event.message.replyEmbeds(playlistEmbed(result.tracks)).queue()
    }

    override fun onSearchResultLoaded(result: SearchResult) {
        val tracks = result.tracks

        if (tracks.isEmpty()) {
            guildMusicManager.metadata?.sendMessage("No tracks found!")?.queue()
            return
        }

        val firstTrack = tracks.first()

        val syncData = PlayerSyncData().apply {
            eventExtend = "event"
            guildId = guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id ?: ""
            voiceChannelId = this@AudioLoader.voiceChannelId
            musicList = listOf(firstTrack.info.title)
            event = PlayerEvent().apply {
                type = "SearchResultLoaded"
                guildId = guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id ?: ""
                channelId = this@AudioLoader.voiceChannelId
            }
        }
        sendPlayerSync(syncData)

        event.message.replyEmbeds(trackEmbed(firstTrack)).queue()
        guildMusicManager.scheduler.enqueue(firstTrack)
    }

    override fun noMatches() {
        event.message.reply("No matches found for your input!").queue()
    }

    override fun loadFailed(result: LoadFailed) {
        event.message.reply("Failed to load track! " + result.exception.message).queue()
        val syncData = PlayerSyncData().apply {
            eventExtend = "event"
            guildId = guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id ?: ""
            voiceChannelId = this@AudioLoader.voiceChannelId
            event = PlayerEvent().apply {
                type = "LoadFailed"
                guildId = guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id ?: ""
                channelId = this@AudioLoader.voiceChannelId
            }
        }

        sendPlayerSync(syncData)
    }

    private fun sendPlayerSync(data: PlayerSyncData) {
        try {
            guildMusicManager.metadata?.asGuildMessageChannel()?.guild?.id?.let {
                AnimalSync.getInstance().send(
                    "player_sync",
                    AnimalSync.getInstance().clientId.toString(),
                    data
                )
            }
        } catch (e: Exception) {
            println("Failed to sync player state: ${e.message}")
        }
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
}
