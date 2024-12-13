package dev.pierrot.commands.core

import dev.pierrot.config
import dev.pierrot.listeners.AnimalSync
import dev.pierrot.service.getLogger
import dev.pierrot.service.setTimeout
import dev.pierrot.service.tempReply
import kotlinx.coroutines.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object MessageHandler {
    private val logger = getLogger("MessageHandler")
    private val animalSync = AnimalSync.getInstance()
    private val pendingCommands = ConcurrentHashMap<String, CommandContext>()
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun handle(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        coroutineScope.launch {
            val context = createMessageContext(event) ?: return@launch

            processCommand(context.command, context)
        }
    }

    private fun processMessage(type: String, message: Map<String, Any>) {
        val messageId = message["messageId"] as? String ?: return
        if (message["connectionId"] != animalSync.clientConnectionId) return

        coroutineScope.launch {
            pendingCommands.remove(messageId)?.let { context ->
                when (type) {
                    "play", "command" -> runCommand(context)
                    "no_client" -> {
                        tempReply(
                            context.event.message,
                            "Hiện tại không có bot nào khả dụng để phát nhạc. Vui lòng thử lại sau."
                        )
                    }
                }
            }
        }
    }

    private fun runCommand(context: CommandContext) {
        handleCommandResult(context.command.execute(context), context)
    }


    private fun findCommand(commandName: String): PrefixCommand? {
        return CommandRegistry.getCommand(commandName)
    }

    private fun handleMusicCommand(context: CommandContext) = runBlocking {
        animalSync.send(
            "sync_play",
            context.event.messageId,
            context.event.member?.voiceState?.channel?.id,
            context.event.guild.id,
            context.event.channel.id,
            context.args
        )
    }

    private fun handleRegularCommand(context: CommandContext) = runBlocking {
        animalSync.send(
            "command_sync",
            context.event.messageId,
            context.event.guild.id,
            context.event.channel.id,
            context.event.member?.voiceState?.channel?.id
        )
    }

    private fun createMessageContext(event: MessageReceivedEvent): CommandContext? {
        val (prefix, isMentionPrefix) = determinePrefix(event)
        val content = event.message.contentRaw
        if (!content.startsWith(prefix, ignoreCase = true)) return null

        val withoutPrefix = content.substring(prefix.length).trim()
        val args = withoutPrefix.split("\\s+".toRegex())
        if (args.isEmpty()) return null
        val commandName = args[0].lowercase()

        val command = findCommand(commandName) ?: run {
            handleUnknownCommand(event, isMentionPrefix)
            return null
        }

        return CommandContext(
            event = event,
            prefix = prefix,
            isMentionPrefix = isMentionPrefix,
            command = command,
            args = args.drop(1),
            rawArgs = withoutPrefix.substringAfter(args[0]).trim(),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun processCommand(command: PrefixCommand, context: CommandContext) = runBlocking {
        if (!validateVoiceRequirements(command, context)) return@runBlocking
        if (!animalSync.isConnect()) runCommand(context).also { return@runBlocking }
        pendingCommands[context.event.messageId] = context

        try {
            withTimeout(10_000) {
                if (command.commandConfig.category.equals("music", ignoreCase = true)) {
                    handleMusicCommand(context)
                } else {
                    handleRegularCommand(context)
                }
            }
        } catch (e: TimeoutCancellationException) {
            logger.warn("Command timed out: ${context.command}")
            tempReply(context.event.message, "⏳ | Lệnh thực thi quá lâu, vui lòng thử lại.")
        } catch (e: Exception) {
            logger.error("Error processing command: ", e)
            tempReply(context.event.message, "❌ | Đã xảy ra lỗi: ${e.message}")
        } finally {
            setTimeout(10_000) { pendingCommands.remove(context.event.messageId) }
        }
    }

    private fun validateVoiceRequirements(command: PrefixCommand, context: CommandContext): Boolean {
        val needsVoice = command.commandConfig.voiceChannel ||
                command.commandConfig.category.equals("music", ignoreCase = true)

        val memberVoiceState = context.event.member?.voiceState

        return when {
            !needsVoice -> true
            memberVoiceState?.channel == null -> false
            else -> true
        }
    }

    private fun determinePrefix(event: MessageReceivedEvent): Pair<String, Boolean> {
        val mention = event.message.mentions.users.firstOrNull { it.id == event.jda.selfUser.id }
        return if (mention != null) mention.asMention to true else config.app.prefix to false
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
    ) {
        when (result) {
            is CommandResult.Success -> Unit
            is CommandResult.Error -> sendErrorEmbed(context.event.message, result.message)
            is CommandResult.CooldownActive -> {
                val timeStamp = "<t:${(result.remainingTime.toMillis()).toInt()}:R>"
                tempReply(
                    context.event.message,
                    "⏳ | Hãy đợi $timeStamp để sử dụng lệnh.",
                    result.remainingTime.toMillis()
                )
            }

            CommandResult.InsufficientPermissions -> Unit
            CommandResult.InvalidArguments -> tempReply(
                context.event.message,
                "Sai cách dùng lệnh, cách dùng đúng: ${context.command.commandConfig.usage}"
            )
        }
    }

    fun setupCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate({
            val currentTime = System.currentTimeMillis()
            pendingCommands.entries.removeIf { (_, context) ->
                currentTime - context.timestamp > TimeUnit.MINUTES.toMillis(5)
            }
        }, 1, 1, TimeUnit.MINUTES)
    }

    fun setupPermanentSubscriptions() {
        animalSync.onMap("play") { message -> processMessage("play", message) }
        animalSync.onMap("no_client") { message -> processMessage("no_client", message) }
        animalSync.onMap("command") { message -> processMessage("command", message) }
    }

    private fun sendErrorEmbed(message: Message, error: String, delay: Long = 20_000) {
        val embed = EmbedBuilder()
            .setDescription("❌ | Có lỗi xảy ra: \n```\n${error.take(2000)}\n```")
            .setColor(Color.RED)
            .build()

        tempReply(message, embed, delay)
    }
}
