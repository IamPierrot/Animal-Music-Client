package dev.pierrot.commands.core

import dev.pierrot.config
import dev.pierrot.getLogger
import dev.pierrot.listeners.AnimalSync
import dev.pierrot.setTimeout
import dev.pierrot.tempReply
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

    init {
        setupPermanentSubscriptions()
        setupCleanupTask()
    }

    private fun setupCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate({
            synchronized(pendingCommands) {
                val currentTime = System.currentTimeMillis()
                pendingCommands.entries.removeIf { (_, context) ->
                    currentTime - context.timestamp > TimeUnit.MINUTES.toMillis(5)
                }
            }
        }, 1, 1, TimeUnit.MINUTES)
    }

    private fun setupPermanentSubscriptions() {
        animalSync.onMap("play") { message ->
            processMessage("play", message)
        }

        animalSync.onMap("no_client") { message ->
            processMessage("no_client", message)
        }

        animalSync.onMap("command") { message ->
            processMessage("command", message)
        }
    }

    private fun processMessage(type: String, message: Map<String, Any>) {
        val messageId = message["messageId"] as? String ?: return
        if (message["connectionId"] != animalSync.clientConnectionId) return

        coroutineScope.launch {
            pendingCommands.computeIfPresent(messageId) { _, context ->
                when (type) {
                    "play" -> {
                        findCommand(context.commandName)?.let { handleCommandResult(it.execute(context), context, it) }
                    }

                    "no_client" -> {
                        context.event.channel.sendMessage(
                            "Hiện tại không có bot nào khả dụng để phát nhạc. Vui lòng thử lại sau."
                        ).queue()
                    }

                    "command" -> {
                        findCommand(context.commandName)?.let { handleCommandResult(it.execute(context), context, it) }
                    }
                }
                null
            }
        }
    }

    fun handle(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        coroutineScope.launch {
            val messageContext = createMessageContext(event) ?: return@launch
            val command = findCommand(messageContext.commandName) ?: run {
                handleUnknownCommand(event, messageContext.isMentionPrefix)
                return@launch
            }

            processCommand(command, messageContext)
        }
    }

    private fun createMessageContext(event: MessageReceivedEvent): CommandContext? {
        val (prefix, isMentionPrefix) = determinePrefix(event)
        val content = event.message.contentRaw

        if (!content.lowercase().startsWith(prefix.lowercase())) return null

        val withoutPrefix = content.substring(prefix.length).trim()
        val args = withoutPrefix.split("\\s+".toRegex())
        if (args.isEmpty()) return null

        return CommandContext(
            event = event,
            prefix = prefix,
            isMentionPrefix = isMentionPrefix,
            commandName = args[0].lowercase(),
            args = args.drop(1),
            rawArgs = withoutPrefix.substringAfter(args[0]).trim(),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun findCommand(commandName: String): PrefixCommand? {
        return CommandRegistry.getCommand(commandName)
    }

    private suspend fun processCommand(command: PrefixCommand, context: CommandContext) {
        if (!validateVoiceRequirements(command, context)) return

        if (context.isMentionPrefix) {
            handleCommandResult(command.execute(context), context, command)
            return
        }

        pendingCommands[context.event.messageId] = context

        coroutineScope.launch(Dispatchers.IO) {
            try {
                withTimeout(10000) {
                    if (command.commandConfig.category == "music") {
                        handleMusicCommand(context)
                    } else {
                        handleRegularCommand(context)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                logger.warn("Command timed out: ${context.commandName}")
            } catch (e: Exception) {
                logger.error("Error processing command: ${e.message}", e)
            } finally {
                setTimeout(5000) { pendingCommands.remove(context.event.messageId) }
            }
        }
    }

    private suspend fun handleMusicCommand(context: CommandContext) {
        animalSync.send(
            "sync_play",
            context.event.messageId,
            context.event.member?.voiceState?.channel?.id,
            context.event.guild.id,
            context.event.channel.id,
            context.args
        )
    }

    private suspend fun handleRegularCommand(context: CommandContext) {
        animalSync.send(
            "command_sync",
            context.event.messageId,
            context.event.guild.id,
            context.event.channel.id,
            context.event.member?.voiceState?.channel?.id
        )
    }

    private fun validateVoiceRequirements(command: PrefixCommand, context: CommandContext): Boolean {
        val needsVoice = command.commandConfig.voiceChannel ||
                command.commandConfig.category.lowercase() == "music"

        if (!needsVoice) return true

        val memberVoiceState = context.event.member?.voiceState

        if (command.commandConfig.category == "music") {
            val selfVoiceState = context.event.guild.selfMember.voiceState
            if (selfVoiceState?.channel?.id != null &&
                memberVoiceState?.channel?.id != selfVoiceState.channel?.id
            ) {
                return false
            }
        }

        return true
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
                tempReply(
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
                    "Sai cách dùng lệnh, cách dùng đúng: ${prefixCommand.commandConfig.usage}"
                )
        }
    }

    private fun sendErrorEmbed(message: Message, error: String, delay: Long = 20000) {
        val embed = EmbedBuilder()
            .setDescription("❌ | Có lỗi xảy ra: \n```\n${error.take(2000)}\n```")
            .setColor(Color.RED)
            .build()

        tempReply(message, embed, delay)
    }
}