package dev.pierrot.commands.prefix

import dev.pierrot.App
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.config
import dev.pierrot.getOrCreateMusicManager
import dev.pierrot.handlers.AudioLoader
import dev.pierrot.joinHelper
import net.dv8tion.jda.api.Permission
import java.time.Duration

class Play : BasePrefixCommand() {
    override val name: String = "play"
    override val description: String = "chơi nhạc"
    override val aliases: Array<String> = arrayOf("p")
    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withBotPermissions(listOf(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
            .withCooldown(Duration.ofSeconds(5))
            .withUsage( "${config.app.prefix} $name <tên bài hát | link youtube/spotify>")
            .withCategory("music")
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        val guild = context.event.guild

        joinHelper(context.event)

        if (context.args.isEmpty()) {
            return CommandResult.InvalidArguments
        }

        val identifier = context.args.joinToString(" ")

        val query = if (identifier.startsWith("https")) identifier else "ytsearch:$identifier"

        val guildId = guild.id
        val link = App.ServiceLocator.lavalinkClient.getOrCreateLink(guildId.toLong())
        val guildMusicManager = getOrCreateMusicManager(guildId, context.event.channel)

        link.loadItem(query).subscribe(AudioLoader(context.event, guildMusicManager))
        return CommandResult.Success
    }

}