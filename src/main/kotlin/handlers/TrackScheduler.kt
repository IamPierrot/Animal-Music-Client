package dev.pierrot.handlers

import dev.arbjerg.lavalink.client.event.TrackEndEvent
import dev.arbjerg.lavalink.client.event.TrackStartEvent
import dev.arbjerg.lavalink.client.player.Track
import dev.pierrot.service.LoopMode
import dev.pierrot.service.embed
import dev.pierrot.service.getLogger
import dev.pierrot.service.setTimeout
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.slf4j.Logger
import java.awt.Color
import java.util.*

class TrackScheduler(private val guildMusicManager: GuildMusicManager) {
    val queue: Queue<Track> = LinkedList()
    val history: Deque<Track> = ArrayDeque()
    private val logger: Logger = getLogger(TrackScheduler::class.java)
    private var loopMode: LoopMode = LoopMode.NONE
    private var currentTrack: Track? = null
    private var goingBack = false
    private var previousTrack: Track? = null

    fun onTrackStart(event: TrackStartEvent) {
        val track = event.track
        currentTrack = track
        previousTrack = currentTrack

        logger.info("Track started: {}", track.info)

        val row = ActionRow.of(
            Button.secondary("back", Emoji.fromCustom("firsttrack", 1274012659230183466L, false)),
            Button.secondary("loop", Emoji.fromCustom("loop", 1274012305281122379L, false)),
            Button.secondary("stop", Emoji.fromCustom("stop", 1274012523120820309L, false)),
            Button.secondary("pause", Emoji.fromCustom("pause", 1274012623414886521L, false)),
            Button.secondary("skip", Emoji.fromCustom("next", 1274012904038862879L, false))
        ).components

        try {
            guildMusicManager.metadata?.sendMessageEmbeds(trackEmbed(track))?.addActionRow(row)?.complete()
                ?.let { msg ->
                setTimeout(track.info.length) { msg.delete().queue() }
            }
        } catch (e: Exception) {
            logger.error("Error while sending music card: {}", e.message, e)
        }
    }

    fun onTrackEnd(event: TrackEndEvent) {
        val endReason = event.endReason

        if (!goingBack) {
            currentTrack?.let {
                if (history.size > 50) history.clear()
                history.push(it)
            }
        }
        goingBack = false

        if (endReason.mayStartNext) {
            when (loopMode) {
                LoopMode.TRACK -> startTrack(event.track.makeClone())
                LoopMode.QUEUE -> handleQueueLoop(event.track)
                LoopMode.NONE -> nextTrack()
            }
        }
    }

    private fun handleQueueLoop(currentTrack: Track) {
        if (queue.isEmpty()) {
            if (history.isEmpty()) startTrack(currentTrack.makeClone()).also { return }

            queue.addAll(history.reversed())
            history.clear()
        }
        nextTrack()
    }

    @Synchronized
    fun enqueue(track: Track) {
        val lavalinkPlayer = guildMusicManager.getPlayer().orElse(null)
        if (lavalinkPlayer?.track == null) startTrack(track) else queue.offer(track)
    }

    @Synchronized
    fun enqueuePlaylist(tracks: List<Track>) {
        val lavalinkPlayer = guildMusicManager.getPlayer().orElse(null)
        queue.addAll(tracks)
        if (lavalinkPlayer?.track == null) startTrack(queue.poll())
    }

    @Synchronized
    fun skipTrack() {
        goingBack = false
        nextTrack()
    }

    @Synchronized
    fun backTrack() {
        if (history.isNotEmpty()) {
            goingBack = true
            val previousTrack = history.pop()
            currentTrack?.let { queue.offer(it) }
            startTrack(previousTrack)
        } else {
            guildMusicManager.metadata?.sendMessageEmbeds(
                EmbedBuilder()
                    .setAuthor("Không còn bài hát nào trong lịch sử!")
                    .setColor(Color.RED)
                    .build()
            )?.queue()
        }
    }

    @Synchronized
    private fun startTrack(track: Track?) {
        guildMusicManager.getLink().ifPresent { link ->
            link.createOrUpdatePlayer()
                .setTrack(track)
                .setVolume(35)
                .subscribe()
        }
    }

    @Synchronized
    fun removeTrack(index: Int) {
        val temp = ArrayList(queue)
        if (index in temp.indices) {
            temp.removeAt(index)
            queue.clear()
            queue.addAll(temp)
        }
    }

    @Synchronized
    private fun nextTrack() {
        val nextTrack = queue.poll()

        if (nextTrack != null) {
            startTrack(nextTrack)
        } else {
            if (loopMode == LoopMode.TRACK) {
                startTrack(currentTrack?.makeClone())
                return
            }

            currentTrack = null
            guildMusicManager.metadata?.sendMessageEmbeds(
                embed()
                    .setAuthor("Không còn bài hát nào trong danh sách!")
                    .setColor(Color.RED)
                    .build()
            )?.queue()
            startTrack(null)
        }
    }

    @Synchronized
    fun getLoopMode(): Int = loopMode.ordinal

    @Synchronized
    fun setLoopMode(loopMode: LoopMode) {
        this.loopMode = loopMode
    }

    @Synchronized
    fun changeLoopMode(loopMode: Int) {
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
    }

    private fun trackEmbed(track: Track): MessageEmbed {
        val trackInfo = track.info
        val lengthInMillis = trackInfo.length
        val minutes = (lengthInMillis / 1000) / 60
        val seconds = (lengthInMillis / 1000) % 60

        return embed()
            .setAuthor("MENU ĐIỀU KHIỂN", null, trackInfo.artworkUrl)
            .setDescription(
                """
                    :notes: **[${trackInfo.title}](${trackInfo.uri})**
                    :musical_keyboard: **Tác giả :** `${trackInfo.author}`
                    :hourglass: **Thời lượng :** `$minutes:$seconds`
                """.trimIndent()
            )
            .setFooter("💖 Âm nhạc đi trước tình yêu theo sau", guildMusicManager.metadata?.jda?.selfUser?.avatarUrl)
            .setThumbnail(trackInfo.artworkUrl)
            .build()
    }
}