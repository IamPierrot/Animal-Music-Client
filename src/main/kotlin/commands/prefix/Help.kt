package dev.pierrot.commands.prefix

import dev.pierrot.commands.base.BasePrefixCommand
import dev.pierrot.commands.config.CommandConfig
import dev.pierrot.commands.core.CommandContext
import dev.pierrot.commands.core.CommandRegistry
import dev.pierrot.commands.core.CommandResult
import dev.pierrot.commands.core.PrefixCommand
import dev.pierrot.config
import dev.pierrot.embed
import dev.pierrot.tempReply
import dev.pierrot.toCapital
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed

class Help : BasePrefixCommand() {
    override val name: String = "help"
    override val description: String = "Danh sách lệnh"
    override val aliases: Array<String> = arrayOf("help")
    override val commandConfig
        get() = CommandConfig.Builder()
            .withCategory("Info")
            .withUsage("$name <tên command>")
            .build()

    override fun executeCommand(context: CommandContext): CommandResult {
        if (context.args.isEmpty()) {
            val commandsEmbed = embed()
                .setAuthor("Danh sách lệnh", null, context.event.author.avatarUrl)
                .setFooter("Music comes first, love follows 💖", context.event.author.avatarUrl)
                .setDescription(
                    """
                        - Ckao` ${context.event.author.asMention}, mình là bot âm nhạc :3
                        - Bot vẫn đang trong quá trình phát triển :3, có lỗi gì dí thằng dev ở bio (hoặc donate cho nó đi ._.).
                        ### Danh sách lệnh
                        > Có thể dùng ${config.app.prefix} help <tên lệnh> để biết thêm chi tiết. 
                    """.trimIndent()
                )

            val mapCommands: Map<String, PrefixCommand> = CommandRegistry.commands.toMap()
            val categoryGroupedCommands: Map<String, List<String>> =
                mapCommands.values.groupBy { it.commandConfig.category }
                    .mapValues { listEntry -> listEntry.value.map { it.name }.sorted() }

            commandsEmbed.fields.addAll(categoryGroupedCommands.map { (category, commands) ->
                val commandChunks = commands.chunked(7)
                val formattedCommands = commandChunks.joinToString("\n") { chunk ->
                    chunk.joinToString(" ") { "**`$it`**" }
                }
                MessageEmbed.Field(
                    category.toCapital(),
                    formattedCommands,
                    false
                )
            }.toMutableList())
            context.event.message.replyEmbeds(commandsEmbed.build()).queue()
            return CommandResult.Success
        }

        val commandName = context.args[0]
        val command = CommandRegistry.getCommand(commandName)

        if (commandName.isBlank() || command == null) {
            tempReply(context.event.message, "❌ | Không tìm thấy $commandName")
            return CommandResult.Success
        }

        val helpCommandEmbed = embed()
            .setAuthor("Hướng dẫn sử dụng command $name", null, context.event.jda.selfUser.avatarUrl)
            .setTitle("Tên lệnh: $commandName")
            .setDescription(
                """
                - **Mô tả lệnh:** _\`${command.description}\`_
                - **Cách dùng lệnh:** `${command.commandConfig.usage}`
                - **Cách dùng nhanh khác:** **`${command.commandConfig.prefix}`** ${command.aliases.joinToString(" | ") { "**`$it`**" }}
            """.trimIndent()
            )
            .addField("User Permissions", resolvePermissionString(command.commandConfig.requireUserPermissions), true)
            .addField("Client Permissions", resolvePermissionString(command.commandConfig.requireBotPermissions), true)
            .setFooter("Music comes first, love follows 💖", context.event.author.avatarUrl)
            .build()

        context.event.message.replyEmbeds(helpCommandEmbed).queue()
        return CommandResult.Success
    }

    private fun resolvePermissionString(permissions: List<Permission>): String {
        return if (permissions.isNotEmpty()) permissions.joinToString(" ") { "**`${it.name}`**" } else "**`Không có`**"
    }
}