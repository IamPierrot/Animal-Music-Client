package dev.pierrot.service

import dev.pierrot.App
import dev.pierrot.handlers.GuildMusicManager
import dev.pierrot.listeners.JDAListener
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*


enum class LoopMode {
    NONE,
    TRACK,
    QUEUE
}

fun getOrCreateMusicManager(guildId: String, metadata: MessageChannelUnion? = null): GuildMusicManager {
    synchronized(JDAListener::class.java) {
        val guildMusicManager = App.ServiceLocator.musicManagerStrategy.getMusicManager(guildId)

        if (metadata != null && guildMusicManager.metadata?.id !== metadata.id) {
            guildMusicManager.metadata = metadata
        }
        return guildMusicManager
    }
}

fun joinHelper(event: MessageReceivedEvent) {
    val member = checkNotNull(event.member)
    val memberVoiceState = checkNotNull(member.voiceState)

    if (memberVoiceState.inAudioChannel()) {
        Objects.requireNonNull(memberVoiceState.channel)?.let { event.jda.directAudioController.connect(it) }
    }

    getOrCreateMusicManager(member.guild.id, event.channel)
}