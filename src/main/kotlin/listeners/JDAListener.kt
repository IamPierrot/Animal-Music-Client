package dev.pierrot.listeners

import dev.pierrot.commands.core.CommandRegistry
import dev.pierrot.commands.core.MessageHandler
import dev.pierrot.getLogger
import dev.pierrot.models.PlayerEvent
import dev.pierrot.models.PlayerSyncData
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class JDAListener : ListenerAdapter() {
    companion object {
        private val logger = getLogger(JDAListener::class.java)
        private val animalSync = AnimalSync.getInstance()
    }

    override fun onReady(event: ReadyEvent) {
        logger.info("{} is ready!", event.jda.selfUser.asTag)
        val guilds =  event.jda.guilds.map { it.id }
        animalSync.send("guild_sync", animalSync.clientId.toString(), guilds)

        CommandRegistry.loadCommands()
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        val guilds =  event.jda.guilds.map { it.id }
        animalSync.send("GuildSync", animalSync.clientId.toString(), guilds)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        val guilds =  event.jda.guilds.map { it.id }
        animalSync.send("GuildSync", animalSync.clientId.toString(), guilds)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        MessageHandler.handle(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val selfUser = event.guild.selfMember

        if (event.channelJoined != null && event.member == selfUser) {
            handleVoiceJoin(event)
        }

        if (event.channelLeft != null && event.member == selfUser) {
            handleVoiceLeave(event)
        }
    }

    private fun handleVoiceJoin(event: GuildVoiceUpdateEvent) {
        val syncData = PlayerSyncData(
            eventExtend = "event",
            guildId = event.guild.id,
            voiceChannelId = event.channelJoined?.id ?: "",
            event = PlayerEvent(
                type = "join",
                guildId = event.guild.id,
                channelId = event.channelJoined?.id ?: ""
            )
        )

        try {
            animalSync.send(
                "player_sync",
                animalSync.clientId.toString(),
                syncData
            )
        } catch (e: Exception) {
            logger.error("Failed to sync voice join: {}", e.message)
        }
    }

    private fun handleVoiceLeave(event: GuildVoiceUpdateEvent) {
        val syncData = PlayerSyncData(
            eventExtend = "event",
            guildId = event.guild.id,
            voiceChannelId = event.channelLeft?.id ?: "",
            event = PlayerEvent(
                type = "left",
                guildId = event.guild.id,
                channelId = event.channelLeft?.id ?: ""
            )
        )

        try {
            animalSync.send(
                "player_sync",
                animalSync.clientId.toString(),
                syncData
            )
        } catch (e: Exception) {
            logger.error("Failed to sync voice leave: {}", e.message)
        }
    }

}