package dev.pierrot.commands.base

import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.commands.core.CooldownManager
import dev.pierrot.commands.core.PrefixCommand
import dev.pierrot.commands.types.CooldownScopes
import dev.pierrot.service.getLogger
import dev.pierrot.service.tempReply
import org.slf4j.Logger
import java.time.Duration

// Base Command Implementation (Template Method Pattern)
abstract class BasePrefixCommand : PrefixCommand {
    private val logger: Logger = getLogger(this::class.java)
    override val commandConfig: CommandConfig = CommandConfig.Builder().build()
    protected open val cooldownScope: CooldownScopes = CooldownScopes.USER

    private val cooldownManager = CooldownManager()

    final override fun execute(context: CommandContext): CommandResult {
        try {
            val memberVoiceState = context.event.member?.voiceState
            val selfVoiceState = context.event.guild.selfMember.voiceState
            if (context.command.commandConfig.category.equals("music", ignoreCase = true)) {
                if (memberVoiceState?.channel == null) {
                    return CommandResult.Error("❌ | Bạn cần ở trong voice channel để sử dụng lệnh này.")
                } else if (selfVoiceState?.channel != null && memberVoiceState.channel?.id != selfVoiceState.channel?.id) {
                    return CommandResult.Error("❌ | Bạn cần ở cùng voice channel với bot để sử dụng lệnh này.")
                }
            }



            // Check permissions
            val permissionResult = checkPermissions(context)
            if (permissionResult != CommandResult.Success) return permissionResult

            // Check cooldown
            val cooldownKey = cooldownScope.getKey(context)
            val remainingCooldown = cooldownManager.getRemainingCooldown(cooldownKey)
            if (remainingCooldown > Duration.ZERO) return CommandResult.CooldownActive(remainingCooldown)


            // Execute command logic
            return executeCommand(context).also { result ->
                if (result is CommandResult.Success) {
                    cooldownManager.applyCooldown(cooldownKey, commandConfig.cooldown)
                    if (commandConfig.deleteCommandMessage) context.event.message.delete().queue(null) { error ->
                        logger.warn("Không thể xóa tin nhắn: ${error.message}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Lỗi khi thực thi lệnh: ", e)
            return CommandResult.Error("❌ | Đã xảy ra lỗi khi thực hiện lệnh: ${e.message}")
        }
    }

    protected abstract fun executeCommand(context: CommandContext): CommandResult

    private fun checkPermissions(context: CommandContext): CommandResult {
        val guild = context.event.guild
        val member = context.event.member ?: return CommandResult.InsufficientPermissions
        val selfMember = guild.selfMember

        // Check bot permissions
        val missingBotPermissions = commandConfig.requireBotPermissions.filter { !selfMember.hasPermission(it) }
        if (missingBotPermissions.isNotEmpty()) tempReply(
            context.event.message,
            "❌ | Bot thiếu những quyền sau: ${missingBotPermissions.joinToString(" ") { "`$it`" }}."
        ).also { return CommandResult.InsufficientPermissions }

        // Check user permissions
        val missingUserPermissions = commandConfig.requireUserPermissions.filter { !member.hasPermission(it) }
        if (missingUserPermissions.isNotEmpty()) tempReply(
            context.event.message,
            "❌ | Bạn thiếu những quyền sau: ${missingUserPermissions.joinToString(" ") { "`$it`" }}."
        ).also { CommandResult.InsufficientPermissions }

        return CommandResult.Success
    }
}
