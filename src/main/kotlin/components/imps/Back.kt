package dev.pierrot.components.imps

import dev.pierrot.components.core.ButtonComponent
import dev.pierrot.service.getOrCreateMusicManager
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class Back : ButtonComponent(
    "back",
    true
) {

    override fun execute(event: ButtonInteractionEvent) {
        try {
            getOrCreateMusicManager(event.guild!!.id).back()
            event.interaction.reply("Trở về track phía trước!").queue()
        } catch (e: Exception) {
            event.interaction.reply("❌ | có lỗi khi trở về quá khứ!").queue()

        }
    }
}