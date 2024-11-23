package dev.pierrot

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.NodeOptions
import dev.arbjerg.lavalink.client.event.*
import dev.arbjerg.lavalink.client.getUserIdFromToken
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
import dev.pierrot.handlers.GuildMusicManager
import dev.pierrot.listeners.JDAListener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.concurrent.ConcurrentHashMap

class App private constructor() {
    companion object {
        @Volatile
        private var instance: App? = null

        fun getInstance(): App =
            instance ?: synchronized(this) {
                instance ?: App().also { instance = it }
            }
    }

    class JDAFactory {
        fun createJDA(token: String, lavalinkClient: LavalinkClient): JDA {
            return JDABuilder.createDefault(token)
                .setVoiceDispatchInterceptor(JDAVoiceUpdateListener(lavalinkClient))
                .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .enableCache(CacheFlag.VOICE_STATE)
                .addEventListeners(JDAListener())
                .build()
                .awaitReady()
        }
    }

    class LavalinkNodeBuilder {
        private var name: String = "default"
        private var serverUri: String = ""
        private var password: String = ""

        fun setName(name: String) = apply { this.name = name }
        fun setServerUri(uri: String) = apply { this.serverUri = uri }
        fun setPassword(password: String) = apply { this.password = password }

        fun build(): NodeOptions {
            return NodeOptions.Builder()
                .setName(name)
                .setServerUri(serverUri)
                .setPassword(password)
                .build()
        }
    }

    interface LavalinkEventObserver {
        fun onTrackStart(event: TrackStartEvent)
        fun onTrackEnd(event: TrackEndEvent)
        fun onWebSocketClosed(event: WebSocketClosedEvent)
        fun onReady(event: ReadyEvent)
        fun onStats(event: StatsEvent)
    }
    interface MusicManagerStrategy {
        fun getMusicManager(guildId: String): GuildMusicManager
        fun removeMusicManager(guildId: String)
    }

    class ConcurrentMusicManagerStrategy : MusicManagerStrategy {
        private val musicManagers = ConcurrentHashMap<String, GuildMusicManager>()

        override fun getMusicManager(guildId: String): GuildMusicManager {
            return musicManagers.computeIfAbsent(guildId) { GuildMusicManager(guildId, null) }
        }

        override fun removeMusicManager(guildId: String) {
            musicManagers.remove(guildId)
        }
    }

    object ServiceLocator {
        lateinit var jda: JDA
        lateinit var lavalinkClient: LavalinkClient
        lateinit var musicManagerStrategy: MusicManagerStrategy
    }

    class LavalinkEvent : LavalinkEventObserver {
        private val logger = getLogger("LavalinkEvent")
        private val SESSION_INVALID: Int = 4006

        override fun onTrackStart(event: TrackStartEvent) {
            logger.info("Track started: {}", event.track.info)
            ServiceLocator.musicManagerStrategy.getMusicManager(event.guildId.toString())
                .scheduler.onTrackStart(event)
        }

        override fun onTrackEnd(event: TrackEndEvent) {
            ServiceLocator.musicManagerStrategy.getMusicManager(event.guildId.toString())
                .scheduler.onTrackEnd(event)
        }

        override fun onWebSocketClosed(event: WebSocketClosedEvent) {
            if (event.code == SESSION_INVALID) {
                val guild = ServiceLocator.jda.getGuildById(event.guildId) ?: return
                val voiceState = guild.selfMember.voiceState ?: return
                val connectedChannel = voiceState.channel ?: return
                ServiceLocator.jda.directAudioController.reconnect(connectedChannel)
            }
        }

        override fun onReady(event: ReadyEvent) {
            logger.info("Node '{}' is ready, session id is '{}'!", event.node.name, event.sessionId)
        }

        override fun onStats(event: StatsEvent) {
            logger.info(
                "Node '{}' stats: {}/{} players (links: {})",
                event.node.name,
                event.playingPlayers,
                event.players,
                ServiceLocator.lavalinkClient.links.size
            )
        }
    }

    init {
        ServiceLocator.lavalinkClient = LavalinkClient(getUserIdFromToken(config.app.token))
        ServiceLocator.jda = JDAFactory().createJDA(config.app.token, ServiceLocator.lavalinkClient)
        ServiceLocator.musicManagerStrategy = ConcurrentMusicManagerStrategy()

        setupLavalink()
    }

    private fun setupLavalink() {
        val node = LavalinkNodeBuilder()
            .setName("localhost")
            .setServerUri(config.app.lavaLinkUrl)
            .setPassword("youshallnotpass")
            .build()

        ServiceLocator.lavalinkClient.addNode(node)

        val eventHandler = LavalinkEvent()
        with(ServiceLocator.lavalinkClient) {
            on(TrackStartEvent::class.java).subscribe(eventHandler::onTrackStart)
            on(TrackEndEvent::class.java).subscribe(eventHandler::onTrackEnd)
            on(WebSocketClosedEvent::class.java).subscribe(eventHandler::onWebSocketClosed)
            on(ReadyEvent::class.java).subscribe(eventHandler::onReady)
            on(StatsEvent::class.java).subscribe(eventHandler::onStats)
        }
    }
}