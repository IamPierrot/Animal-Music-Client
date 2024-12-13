package dev.pierrot.service

import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent

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
