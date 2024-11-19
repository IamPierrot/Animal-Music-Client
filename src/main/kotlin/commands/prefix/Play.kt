package dev.pierrot.commands.prefix

import dev.pierrot.App
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.config
import dev.pierrot.getOrCreateMusicManager
import dev.pierrot.handlers.AudioLoader
import dev.pierrot.listeners.AnimalSync
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class Play : BasePrefixCommand() {
    private val pendingResponses = mutableMapOf<String, CompletableFuture<Boolean>>()

    init {
        val animalSync = AnimalSync.getInstance()

        animalSync.registerMessageHandler("msg") { message ->
            val messageId = message["messageId"] as String
            val guildId = message["guildId"] as String
            val textChannelId = message["textChannelId"] as String

            @Suppress("UNCHECKED_CAST")
            val args = message["args"] as List<String>

            handleSyncMessage(messageId, guildId, textChannelId, args)
        }

        animalSync.registerMessageHandler("handle_no_client") { message ->
            val messageId = message["messageId"] as String
            handleNoClient(messageId)
        }
    }

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

        val guild = context.event.guild
        val voiceChannel = context.event.member?.voiceState?.channel
            ?: return CommandResult.Error("Bạn phải vào voice trước")

        val messageId = context.event.messageId
        val responseFuture = CompletableFuture<Boolean>()
        pendingResponses[messageId] = responseFuture

        AnimalSync.getInstance().send(
            "sync_msg",
            messageId,
            voiceChannel.id,
            guild.id,
            context.event.channel.id,
            context.args
        )

        try {
            val hasAvailableBot = responseFuture.get(5, TimeUnit.SECONDS)
            if (!hasAvailableBot) {
                context.event.channel.sendMessage("Hiện tại không có bot nào khả dụng để phát nhạc. Vui lòng thử lại sau.")
                    .queue()
                return CommandResult.Success
            }
            return CommandResult.Success
        } catch (e: Exception) {
            context.event.channel.sendMessage("Có lỗi xảy ra khi đồng bộ hóa lệnh play. Vui lòng thử lại.").queue()
            return CommandResult.Error("Sync timeout")
        } finally {
            pendingResponses.remove(messageId)
        }
    }

    private fun handleSyncMessage(messageId: String, guildId: String, textChannelId: String, args: List<String>) {
        val guild = App.ServiceLocator.jda.getGuildById(guildId) ?: return
        val textChannel = guild.getGuildChannelById(textChannelId) as? MessageChannelUnion ?: return

        val member = guild.selfMember
        val voiceState = member.voiceState
        var voiceChannelId = voiceState?.channel?.id
        if (voiceState?.channel == null) {
            val targetChannel = guild.getVoiceChannelById(textChannelId)?.members
                ?.firstOrNull { it.voiceState?.inAudioChannel() == true }
                ?.voiceState?.channel

            targetChannel?.let { channel ->
                App.ServiceLocator.jda.directAudioController.connect(channel)
                voiceChannelId = channel.id
            }
        }

        val identifier = args.joinToString(" ")
        val query = if (identifier.startsWith("https")) identifier else "ytsearch:$identifier"

        val link = App.ServiceLocator.lavalinkClient.getOrCreateLink(guildId.toLong())
        val guildMusicManager = getOrCreateMusicManager(guildId, textChannel)

        link.loadItem(query).subscribe(AudioLoader(guildMusicManager, voiceChannelId!!))

        pendingResponses[messageId]?.complete(true)
    }

    private fun handleNoClient(messageId: String) {
        pendingResponses[messageId]?.complete(false)
    }
}