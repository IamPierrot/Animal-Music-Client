package dev.pierrot.commands.core

import com.microsoft.signalr.Subscription
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.config
import dev.pierrot.embed
import dev.pierrot.getLogger
import dev.pierrot.listeners.AnimalSync
import dev.pierrot.tempReply
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.util.*

// Message Handler (Facade Pattern)
object MessageHandler {
    private val animalSync = AnimalSync.getInstance()


    fun handle(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val (prefix, isMentionPrefix) = determinePrefix(event)
        val content = event.message.contentRaw

        if (!content.lowercase().startsWith(prefix.lowercase())) return

        val rawArgs = content.substring(prefix.length).trim()
        val args = rawArgs.split("\\s+".toRegex())
        if (args.isEmpty()) return

        val commandName = args[0].lowercase()
        val command = CommandRegistry.getCommand(commandName) ?: run {
            handleUnknownCommand(event, isMentionPrefix)
            return
        }
        val context = CommandContext(
            event = event,
            args = args.drop(1),
            rawArgs = rawArgs.substringAfter(args[0]).trim(),
            prefix = prefix,
            isMentionPrefix = isMentionPrefix
        )

        val guild = event.guild
        val voiceChannel = event.member?.voiceState?.channel

        val messageId = event.messageId

        if (command.commandConfig.voiceChannel || command.commandConfig.category.lowercase(Locale.getDefault()) == "music") {
            val memberVoiceState = event.member?.voiceState
            val selfVoiceState = event.guild.selfMember.voiceState

            if (memberVoiceState?.channel == null) {
                tempReply(
                    event.message,
                    embed()
                        .setAuthor("❌ | Bạn cần vào voice để thực hiện lệnh này!")
                        .build()
                )
                return
            }

            if (command.commandConfig.category == "music") {
                if (selfVoiceState?.channel?.id != null && memberVoiceState.channel?.id != selfVoiceState.channel?.id) return

                assert(voiceChannel != null)

                animalSync.onMap("play") { message ->
                    if (message["messageId"] as String == messageId) {
                        handleCommandResult(command.execute(context), context, command)
                    }
                }

                animalSync.onMap("no_client") { message ->
                    if (message["messageId"] as String == messageId) {
                        context.event.channel.sendMessage("Hiện tại không có bot nào khả dụng để phát nhạc. Vui lòng thử lại sau.")
                            .queue()
                    }
                }
                try {

                    animalSync.send(
                        "sync_play",
                        messageId,
                        voiceChannel?.id,
                        guild.id,
                        event.channel.id,
                        args
                    )
                } catch (error: Exception) {
                    getLogger("PrefixCommand").warn(error.message)
                    return
                }
                return
            }
        }
        animalSync.onMap("command") { message ->
            if (message["messageId"] as String == messageId) {
                handleCommandResult(command.execute(context), context, command)
            }
        }
        try {
                animalSync.send(
                    "command_sync",
                    messageId,
                    guild.id,
                    context.event.channel.id,
                    voiceChannel?.id
                )
        } catch (error: Exception) {
            return
        }
    }

    private fun determinePrefix(event: MessageReceivedEvent): Pair<String, Boolean> {
        event.message.mentions.users.firstOrNull()?.let { mention ->
            if (mention.id == event.jda.selfUser.id) {
                return mention.asMention to true
            }
        }
        return config.app.prefix to false
    }

    private fun handleUnknownCommand(event: MessageReceivedEvent, isMentionPrefix: Boolean) {
        if (isMentionPrefix) {
            val embed = EmbedBuilder()
                .setDescription(
                    "Chào~ Mình là ca sĩ Isherry:3, prefix của mình là `${config.app.prefix}` hoặc là mention tui để dùng lệnh nè:3.\n" +
                            "Sử dụng `${config.app.prefix}help` để biết toàn bộ lệnh của tui nè :3."
                )
                .setColor(Color.PINK)
                .setFooter("Music comes first, love follows 💞", event.jda.selfUser.avatarUrl)
                .build()

            event.message.replyEmbeds(embed).queue()
        }
    }

    private fun handleCommandResult(result: CommandResult, context: CommandContext, prefixCommand: PrefixCommand) {
        when (result) {
            is CommandResult.Success -> {}
            is CommandResult.Error -> sendErrorEmbed(context.event.message, result.message)
            is CommandResult.CooldownActive -> {
                val timeStamp = "<t:${System.currentTimeMillis() / 1000 + result.remainingTime.seconds}:R>"
                sendErrorEmbed(
                    context.event.message,
                    "Hãy đợi $timeStamp để sử dụng lệnh.",
                    result.remainingTime.toMillis()
                )
            }

            CommandResult.InsufficientPermissions ->
                sendErrorEmbed(context.event.message, "Bạn không đủ quyền dùng lệnh này!")

            CommandResult.InvalidArguments ->
                sendErrorEmbed(
                    context.event.message,
                    "Sai cách dùng lệnh, cách dùng đúng: ${(prefixCommand as? BasePrefixCommand)?.commandConfig?.usage}"
                )
        }
    }

    private fun sendErrorEmbed(message: Message, error: String, delay: Long = 20000) {
        val embed = EmbedBuilder()
            .setDescription("❌ | Có lỗi xảy ra: \n```\n${error.substring(0..error.length / 2)}\n```")
            .setColor(Color.RED)
            .build()

        tempReply(message, embed, delay)
    }
}