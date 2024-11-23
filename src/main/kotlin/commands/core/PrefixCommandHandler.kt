package dev.pierrot.commands.core

import com.microsoft.signalr.Subscription
import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.config
import dev.pierrot.getLogger
import dev.pierrot.listeners.AnimalSync
import dev.pierrot.tempReply
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color

object MessageHandler {
    private val logger = getLogger("MessageHandler")
    private val animalSync = AnimalSync.getInstance()
    private val subscriptions = ArrayList<Subscription>()

    fun handle(event: MessageReceivedEvent) {
        if (!shouldProcessMessage(event)) return

        val messageContext = createMessageContext(event) ?: return
        val command = findCommand(messageContext) ?: run {
            handleUnknownCommand(event, messageContext.isMentionPrefix)
            return
        }

        processCommand(command, messageContext)
    }

    private data class MessageContext(
        val event: MessageReceivedEvent,
        val prefix: String,
        val isMentionPrefix: Boolean,
        val commandName: String,
        val args: List<String>,
        val rawArgs: String
    )

    private fun shouldProcessMessage(event: MessageReceivedEvent): Boolean {
        return !event.author.isBot
    }

    private fun createMessageContext(event: MessageReceivedEvent): MessageContext? {
        val (prefix, isMentionPrefix) = determinePrefix(event)
        val content = event.message.contentRaw

        if (!content.lowercase().startsWith(prefix.lowercase())) return null

        val withoutPrefix = content.substring(prefix.length).trim()
        val args = withoutPrefix.split("\\s+".toRegex())
        if (args.isEmpty()) return null

        return MessageContext(
            event = event,
            prefix = prefix,
            isMentionPrefix = isMentionPrefix,
            commandName = args[0].lowercase(),
            args = args.drop(1),
            rawArgs = withoutPrefix.substringAfter(args[0]).trim()
        )
    }

    private fun findCommand(context: MessageContext): PrefixCommand? {
        return CommandRegistry.getCommand(context.commandName)
    }

    private fun processCommand(command: PrefixCommand, messageContext: MessageContext) {
        val context = CommandContext(
            event = messageContext.event,
            args = messageContext.args,
            rawArgs = messageContext.rawArgs,
            prefix = messageContext.prefix,
            isMentionPrefix = messageContext.isMentionPrefix
        )

        if (!validateVoiceRequirements(command, context)) return

        handleCommandExecution(command, context).run {
            subscriptions.forEach { it.unsubscribe() }
        }
    }

    private fun validateVoiceRequirements(command: PrefixCommand, context: CommandContext): Boolean {
        val needsVoice = command.commandConfig.voiceChannel ||
                command.commandConfig.category.lowercase() == "music"

        if (!needsVoice) return true

        val memberVoiceState = context.event.member?.voiceState
        if (memberVoiceState?.channel == null) {
            sendErrorEmbed(
                context.event.message,
                "❌ | Bạn cần vào voice để thực hiện lệnh này!"
            )
            return false
        }

        if (command.commandConfig.category == "music") {
            val selfVoiceState = context.event.guild.selfMember.voiceState
            if (selfVoiceState?.channel?.id != null &&
                memberVoiceState.channel?.id != selfVoiceState.channel?.id
            ) {
                return false
            }

            handleMusicCommand(context)
            return false
        }

        return true
    }

    private fun handleMusicCommand(context: CommandContext) {
        val messageId = context.event.messageId
        val voiceChannel = context.event.member?.voiceState?.channel
        val guild = context.event.guild

        setupMusicSyncHandlers(context, messageId)

        try {
            animalSync.send(
                "sync_play",
                messageId,
                voiceChannel?.id,
                guild.id,
                context.event.channel.id,
                context.args
            )
        } catch (error: Exception) {
            logger.warn("Failed to sync play command: ${error.message}")
        }
    }

    private fun setupMusicSyncHandlers(context: CommandContext, messageId: String) {
        animalSync.onMap("play") { message ->
            if (message["messageId"] as String == messageId) {
                context.event.channel.sendMessage("✅ Đã thêm bài hát vào hàng đợi").queue()
            }
        }?.let { subscriptions.add(it) }

        animalSync.onMap("no_client") { message ->
            if (message["messageId"] as String == messageId) {
                context.event.channel.sendMessage(
                    "Hiện tại không có bot nào khả dụng để phát nhạc. Vui lòng thử lại sau."
                ).queue()
            }
        }?.let { subscriptions.add(it) }
    }

    private fun handleCommandExecution(command: PrefixCommand, context: CommandContext) {
        val messageId = context.event.messageId

        animalSync.onMap("command") { message ->
            if (message["messageId"] as String == messageId) {
                handleCommandResult(command.execute(context), context, command)
            }
        }?.let { subscriptions.add(it) }

        try {
            animalSync.send(
                "command_sync",
                messageId,
                context.event.guild.id,
                context.event.channel.id,
                context.event.member?.voiceState?.channel?.id
            )
        } catch (error: Exception) {
            logger.warn("Failed to sync command: ${error.message}")
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
                    """
                    Chào~ Mình là ca sĩ Isherry:3, prefix của mình là `${config.app.prefix}` hoặc là mention tui để dùng lệnh nè:3.
                    Sử dụng `${config.app.prefix}help` để biết toàn bộ lệnh của tui nè :3.
                    """.trimIndent()
                )
                .setColor(Color.PINK)
                .setFooter("Music comes first, love follows 💞", event.jda.selfUser.avatarUrl)
                .build()

            event.message.replyEmbeds(embed).queue()
        }
    }

    private fun handleCommandResult(
        result: CommandResult,
        context: CommandContext,
        prefixCommand: PrefixCommand
    ) {
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
            .setDescription("❌ | Có lỗi xảy ra: \n```\n${error.take(error.length / 2)}\n```")
            .setColor(Color.RED)
            .build()

        tempReply(message, embed, delay)
    }
}