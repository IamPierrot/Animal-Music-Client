package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.embed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.util.*
import kotlin.math.abs

class Ping : BasePrefixCommand() {
    override val name = "ping"
    override val description = "Kiểm tra độ trễ"
    override val aliases = emptyArray<String>()
    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withCategory("Info")
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val jda: JDA = context.event.jda
        val ping = jda.restPing
        val guild: Guild = context.event.guild
        val selfUser: User = jda.selfUser // Gets the bot's user
        val botUsername = selfUser.name
        val botAvatarUrl = selfUser.avatarUrl

        val guildName = guild.name
        val guildIconUrl = guild.iconUrl

        val embedBuilder = embed()
            .setColor(Color.MAGENTA)
            .setAuthor(botUsername, null, botAvatarUrl)
            .setDescription("```elm\nAPI Latency (Websocket) : ${Math.round(ping.complete().toFloat())}ms\nMessage Latency         : ${abs((Date().time - context.event.message.timeCreated.toInstant().toEpochMilli())).toInt()}ms```",)
            .setFooter(guildName, guildIconUrl)
        context.event.message.replyEmbeds(embedBuilder.build()).queue()

        return CommandResult.Success
    }
}