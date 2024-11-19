package dev.pierrot.listeners

import dev.pierrot.commands.core.CommandRegistry
import dev.pierrot.commands.core.MessageHandler
import dev.pierrot.getLogger
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
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
        animalSync.send("GuildSync", animalSync.clientId.toString(), guilds)

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


}