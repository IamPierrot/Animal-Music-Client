package dev.pierrot.commands.core

import dev.pierrot.config
import dev.pierrot.database.RootDatabase.db
import dev.pierrot.database.schemas.Guilds
import dev.pierrot.database.schemas.Prefixes
import dev.pierrot.listeners.AnimalSync
import dev.pierrot.service.getLogger
import dev.pierrot.service.tempReply
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.support.postgresql.insertOrUpdate
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

object MessageHandler {
    private val logger = getLogger("MessageHandler")
    private val animalSync = AnimalSync.getInstance()

    fun handle(event: MessageReceivedEvent) = runBlocking {
        if (event.author.isBot) return@runBlocking
        val context = createMessageContext(event) ?: return@runBlocking

        db.insertOrUpdate(Guilds) {
            set(it.guildName, event.guild.name)
            set(it.guildId, event.guild.id)
            set(it.guildOwnerId, event.guild.owner?.id)

            onConflict {
                set(it.guildName, event.guild.name)
                set(it.guildOwnerId, event.guild.owner?.id)
            }
        }

        processCommand(context.command, context)
    }

    init {
        setupEvents()
    }

    private val contexts = ConcurrentHashMap<String, CommandContext>()

    private fun setupEvents() {

        animalSync.onMap("play") { message ->
            (message["messageId"] as? String ?: return@onMap).also {
                processMessage("play", it)
            }
        }

        animalSync.onMap("no_client") { message ->
            (message["messageId"] as? String ?: return@onMap).also {
                processMessage("no_client", it)
            }
        }

        animalSync.onMap("command") { message ->
            (message["messageId"] as? String ?: return@onMap).also {
                processMessage("command", it)
            }
        }
    }

    @Synchronized
    private fun updateContext(messageId: String, context: CommandContext) {
        contexts[messageId] = context
    }

    private fun processMessage(type: String, messageId: String) {
        val context = synchronized(contexts) {
            contexts.remove(messageId)
        } ?: return

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

    private fun runCommand(context: CommandContext) {
        handleCommandResult(context.command.execute(context), context)
    }


    private fun findCommand(commandName: String): PrefixCommand? {
        return CommandRegistry.getCommand(commandName)
    }

    private fun handleMusicCommand(context: CommandContext) = runBlocking {
        delay(100)
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
        delay(100)
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

        if (prefix == null) return null

        val withoutPrefix = content.substring(prefix.length).trim()
        if (withoutPrefix.isEmpty()) return null

        val args = withoutPrefix.split("\\s+".toRegex())
        val commandName = args[0].lowercase()
        val command = findCommand(commandName) ?: run {
            handleUnknownCommand(event, isMentionPrefix)
            return null
        }

        val rawArgs = if (args.size > 1) args[1].trim() else ""

        return CommandContext(
            event = event,
            prefix = prefix,
            isMentionPrefix = isMentionPrefix,
            command = command,
            args = if (args.size > 1) args[1].split("\\s+".toRegex()) else emptyList(),
            rawArgs = rawArgs
        )
    }


    private suspend fun processCommand(command: PrefixCommand, context: CommandContext) {
        if (!validateVoiceRequirements(command, context)) return
        if (!animalSync.isConnect()) runCommand(context).also { return }

        val messageId = context.event.messageId

        updateContext(messageId, context)

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

    private fun determinePrefix(event: MessageReceivedEvent): Pair<String?, Boolean> {
        val mention = event.message.mentions.users.firstOrNull { it.id == event.jda.selfUser.id }
        if (mention != null) {
            return mention.asMention to true
        }

        val guildId = event.guild.id
        val prefixInDatabase = db.sequenceOf(Prefixes)
            .find { it.guildId eq guildId }?.prefix

        val content = event.message.contentRaw
        return if (prefixInDatabase != null && content.startsWith(prefixInDatabase, ignoreCase = true)) {
            prefixInDatabase to false
        } else if (content.startsWith(config.app.prefix, ignoreCase = true)) config.app.prefix to false
        else null to false
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

    private fun sendErrorEmbed(message: Message, error: String, delay: Long = 20_000) {
        val embed = EmbedBuilder()
            .setDescription("❌ | Có lỗi xảy ra: \n```\n${error.take(2000)}\n```")
            .setColor(Color.RED)
            .build()

        tempReply(message, embed, delay)
    }
}
