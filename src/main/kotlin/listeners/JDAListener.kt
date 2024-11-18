package dev.pierrot.listeners

import dev.pierrot.commands.core.CommandRegistry
import dev.pierrot.commands.core.MessageHandler
import dev.pierrot.getLogger
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class JDAListener : ListenerAdapter() {
    companion object {
        private val logger = getLogger(JDAListener::class.java)
    }


    override fun onReady(event: ReadyEvent) {
        logger.info("{} is ready!", event.jda.selfUser.asTag)
        CommandRegistry.loadCommands()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        MessageHandler.handle(event)
    }


}