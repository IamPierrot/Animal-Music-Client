package dev.pierrot

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.NodeOptions
import dev.arbjerg.lavalink.client.event.*
import dev.arbjerg.lavalink.client.getUserIdFromToken
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
import dev.pierrot.handlers.GuildMusicManager
import dev.pierrot.listeners.JDAListener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.*

val musicManagers = HashMap<String, GuildMusicManager>()

class App private constructor() {
    companion object {
        lateinit var lavalinkClient: LavalinkClient
        lateinit var jda: JDA

        private const val SESSION_INVALID: Int = 4006
        private val logger = getLogger(App::class.java)

        fun initApp() {
            App()

            loadLavaLinkEvent()
            lavaLinkRegisterEvents()
        }

        private fun lavaLinkRegisterEvents() {
            registerLavalinkNodes()
            registerLavalinkListeners()
        }

        private fun registerLavalinkNodes() {
            lavalinkClient.addNode(
                NodeOptions.Builder()
                    .setName("localhost")
                    .setServerUri("http://localhost:${config.app.port}")
                    .setPassword("youshallnotpass")
                    .build()
            )
        }

        private fun registerLavalinkListeners() {
            lavalinkClient.on(ReadyEvent::class.java).subscribe { event: ReadyEvent ->
                val node = event.node
                logger.info(
                    "Node '{}' is ready, session id is '{}'!",
                    node.name,
                    event.sessionId
                )
            }

            lavalinkClient.on(StatsEvent::class.java).subscribe { event: StatsEvent ->
                val node = event.node
                logger.info(
                    "Node '{}' has stats, current players: {}/{} (link count {})",
                    node.name,
                    event.playingPlayers,
                    event.players,
                    lavalinkClient.links.size
                )
            }

            lavalinkClient.on(TrackStartEvent::class.java).subscribe { event: TrackStartEvent ->
                val node = event.node
                logger.info(
                    "{}: track started: {}",
                    node.name,
                    event.track.info
                )
                Optional.ofNullable(musicManagers[event.guildId.toString()]).ifPresent { guildMusicManager ->
                    guildMusicManager.scheduler.onTrackStart(event)
                }
            }

            lavalinkClient.on<TrackEndEvent>(TrackEndEvent::class.java).subscribe { event: TrackEndEvent ->
                Optional.ofNullable(musicManagers[event.guildId.toString()]).ifPresent { guildMusicManager ->
                    guildMusicManager.scheduler.onTrackEnd(event)
                }
            }

            lavalinkClient.on(EmittedEvent::class.java).subscribe { event: EmittedEvent ->
                val node = event.node
                logger.info(
                    "Node '{}' emitted event: {}",
                    node.name,
                    event
                )
            }
        }

        private fun loadLavaLinkEvent() {
            lavalinkClient.loadBalancer.addPenaltyProvider(VoiceRegionPenaltyProvider())

            lavalinkClient.on(WebSocketClosedEvent::class.java).subscribe { event: WebSocketClosedEvent ->
                if (event.code == SESSION_INVALID) {
                    val guildId = event.guildId
                    val guild = jda.getGuildById(guildId) ?: return@subscribe

                    val connectedChannel =
                        Objects.requireNonNull<GuildVoiceState?>(guild.selfMember.voiceState).channel
                            ?: return@subscribe

                    jda.directAudioController.reconnect(connectedChannel)
                }
            }
        }
    }

    private val token: String = config.app.token

    init {
        lavalinkClient = LavalinkClient(getUserIdFromToken(token))

        jda = JDABuilder.createDefault(token)
            .setVoiceDispatchInterceptor(JDAVoiceUpdateListener(lavalinkClient))
            .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .enableCache(CacheFlag.VOICE_STATE)
            .addEventListeners(JDAListener())
            .build()
            .awaitReady()

    }


}