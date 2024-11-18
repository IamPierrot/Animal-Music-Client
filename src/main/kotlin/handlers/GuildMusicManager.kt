package dev.pierrot.handlers

import dev.arbjerg.lavalink.client.Link
import dev.arbjerg.lavalink.client.player.LavalinkPlayer
import dev.arbjerg.lavalink.client.player.Track
import dev.pierrot.App
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class GuildMusicManager(private val guildId: String, var metadata: MessageChannelUnion) {

    private val lavalinkClient = App.lavalinkClient
    val scheduler = TrackScheduler(this)
    fun getCurrentTrack(): Track? {
        val result = AtomicReference<Track?>()
        getPlayer().let { lavalinkPlayer ->
            result.set(
                lavalinkPlayer.get().track
            )
        }
        return result.get()
    }

    internal fun getLink(): Optional<Link> {
        return Optional.ofNullable(
            lavalinkClient.getLinkIfCached(guildId.toLong())
        )
    }

    fun getPlayer(): Optional<LavalinkPlayer> {
        return getLink().map { link -> link.cachedPlayer }
    }

}