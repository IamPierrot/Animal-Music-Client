package dev.pierrot.commands.prefix

import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.commands.core.PrefixCommand
import dev.pierrot.database.RootDatabase.db
import dev.pierrot.database.schemas.Prefixes
import dev.pierrot.service.tempReply
import net.dv8tion.jda.api.Permission
import org.ktorm.support.postgresql.insertOrUpdate
import java.time.Duration

class ChangePrefix : PrefixCommand {
    override val name: String = "ChangePrefix"
    override val description: String = "Thay đổi prefix command của 1 sv"
    override val aliases: Array<String> = arrayOf("cp")
    override val commandConfig: CommandConfig = CommandConfig.Builder()
        .withCategory("Util")
        .withCooldown(Duration.ofSeconds(20))
        .withUserPermission(listOf(Permission.MANAGE_SERVER))
        .withUsage("$name <prefix>")
        .build()

    override fun execute(context: CommandContext): CommandResult {
        if (context.args.isEmpty()) return CommandResult.InvalidArguments

        val newPrefix = context.args[0]

        if (newPrefix.isBlank()) return CommandResult.InvalidArguments
        if (newPrefix.length > 5) tempReply(context.event.message, "Quá dài tối đa là (5)")
            .also { return CommandResult.Success }

        db.insertOrUpdate(Prefixes) {
            set(it.guildId, context.event.guild.id)
            set(it.prefix, newPrefix)

            onConflict {
                set(it.prefix, newPrefix)
            }
        }.runCatching { return CommandResult.Error("Có lỗi trong quá trình chuyển đổi prefix") }

        context.event.message.reply("Thay đổi prefix thành công!!")
        return CommandResult.Success
    }


}