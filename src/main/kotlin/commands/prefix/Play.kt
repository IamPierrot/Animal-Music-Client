package dev.pierrot.commands.prefix

import com.microsoft.signalr.Subscription
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class Play : BasePrefixCommand() {
    override val name: String = "play"
    override val description: String = "chơi nhạc"
    override val aliases: Array<String> = arrayOf("p")

    override val commandConfig: CommandConfig
        get() = CommandConfig.Builder()
            .withBotPermissions(listOf(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
            .withCooldown(Duration.ofSeconds(5))
            .withUsage("${config.app.prefix} $name <tên bài hát | link youtube/spotify>")
            .withCategory("music")
            .withRequireVoiceChannel()
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        if (context.args.isEmpty()) {
            return CommandResult.InvalidArguments
        }

        try {
            val guildId = context.event.guild.id

            val voiceChannelId = context.event.member?.voiceState?.id
            joinHelper(context.event)

            val identifier = context.args.joinToString(" ")
            val query = if (identifier.startsWith("https")) identifier else "ytsearch:$identifier"

            val link = App.ServiceLocator.lavalinkClient.getOrCreateLink(guildId.toLong())
            val guildMusicManager = getOrCreateMusicManager(guildId, context.event.channel)

            link.loadItem(query).subscribe(AudioLoader(context.event, guildMusicManager, voiceChannelId!!))
            return CommandResult.Success
        } catch (e: Exception) {
            return CommandResult.Success
        }
    }
}