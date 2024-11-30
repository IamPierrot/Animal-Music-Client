package dev.pierrot.commands.base

import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.commands.core.CooldownManager
import dev.pierrot.commands.core.PrefixCommand
import dev.pierrot.commands.types.CooldownScopes
import dev.pierrot.getLogger
import dev.pierrot.listeners.AnimalSync
import dev.pierrot.tempReply
import org.slf4j.Logger
import java.time.Duration

// Base Command Implementation (Template Method Pattern)
abstract class BasePrefixCommand : PrefixCommand {
    private val logger: Logger = getLogger(this::class.java)
    override val commandConfig: CommandConfig = CommandConfig.Builder().build()
    protected open val cooldownScope: CooldownScopes = CooldownScopes.USER
    protected val animalSync: AnimalSync = AnimalSync.getInstance()

    private val cooldownManager = CooldownManager()

    final override fun execute(context: CommandContext): CommandResult {
        // Check permissions
        val permissionResult = checkPermissions(context)
        if (permissionResult != CommandResult.Success) {
            return permissionResult
        }

        // Check cooldown
        val cooldownKey = cooldownScope.getKey(context)
        val remainingCooldown = cooldownManager.getRemainingCooldown(cooldownKey)
        if (remainingCooldown > Duration.ZERO) {
            return CommandResult.CooldownActive(remainingCooldown)
        }

        val memberVoiceState = context.event.member?.voiceState
        if (memberVoiceState?.channel == null) {
            return CommandResult.Error("❌ | Bạn cần vào voice để thực hiện lệnh này!")
        }

        return try {
            executeCommand(context).also { result ->
                if (result is CommandResult.Success) {
                    cooldownManager.applyCooldown(cooldownKey, commandConfig.cooldown)
                    if (commandConfig.deleteCommandMessage) {
                        context.event.message.delete().queue()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error executing command: ", e)
            CommandResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    protected abstract fun executeCommand(context: CommandContext): CommandResult

    private fun checkPermissions(context: CommandContext): CommandResult {
        val guild = context.event.guild
        val member = context.event.member ?: return CommandResult.InsufficientPermissions
        val selfMember = guild.selfMember

        val missingBotPermissions = commandConfig.requireBotPermissions.filter { !selfMember.hasPermission(it) }
        if (missingBotPermissions.isNotEmpty()) {
            tempReply(
                context.event.message,
                "❌ | Bot thiếu những quyền sau ${missingBotPermissions.joinToString(" ") { "`$it`" }}"
            )
            return CommandResult.InsufficientPermissions
        }

        val missingUserPermissions = commandConfig.requireUserPermissions.filter { !member.hasPermission(it) }
        if (missingUserPermissions.isNotEmpty()) {
            tempReply(
                context.event.message,
                "❌ | Bạn thiếu những quyền sau ${missingUserPermissions.joinToString(" ") { "`$it`" }}"
            )
            return CommandResult.InsufficientPermissions
        }

        return CommandResult.Success
    }
}