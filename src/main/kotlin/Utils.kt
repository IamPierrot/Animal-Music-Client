package dev.pierrot

import dev.pierrot.handlers.GuildMusicManager
import dev.pierrot.listeners.JDAListener
import kotlinx.coroutines.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.GuildVoiceState
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.*

fun getLogger(name: String): Logger {
    return LoggerFactory.getLogger(name)
}

fun getLogger(clazz: Class<*>): Logger {
    return LoggerFactory.getLogger(clazz)
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

fun setTimeout(delayMillis: Long, block: () -> Unit) = runBlocking {
    CoroutineScope(Dispatchers.Default).launch {
        try {
            delay(delayMillis)
            block()
        } catch (e: Exception) {
            getLogger("Error").error("Error: ${e.message}")
        }
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

fun String.toCapital(): String {
    if (this.isEmpty()) {
        return this
    }
    return this.substring(0, 1).uppercase(Locale.getDefault()) + this.substring(1)
}

fun tempReply(context: Message, message: Any, delayMillis: Long = 20000) {
    val sentMessage: Message = when (message) {
        is String -> context.reply(message).complete()
        is MessageEmbed -> context.replyEmbeds(message).complete()
        else -> throw IllegalArgumentException("Unsupported message type: Must be String or MessageEmbed")
    }

    setTimeout(delayMillis) { sentMessage.delete().queue().runCatching {} }
}

fun tempReply(context: GenericComponentInteractionCreateEvent, message: Any, delayMillis: Long = 20000) {
    val sentMessage = when (message) {
        is String -> context.reply(message).setEphemeral(true).complete()
        is MessageEmbed -> context.replyEmbeds(message).setEphemeral(true).complete()
        else -> throw IllegalArgumentException("Unsupported message type: Must be String or MessageEmbed")
    }

    setTimeout(delayMillis) { sentMessage.deleteOriginal().queue().runCatching {} }
}

fun embed(): EmbedBuilder {
    return EmbedBuilder()
        .setColor(Color.pink)
}

enum class LoopMode {
    NONE,
    TRACK,
    QUEUE
}