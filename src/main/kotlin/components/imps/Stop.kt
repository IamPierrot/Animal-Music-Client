package dev.pierrot.components.imps

import dev.pierrot.components.core.ButtonComponent
import dev.pierrot.getOrCreateMusicManager
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class Stop : ButtonComponent(
    "stop",
    true
) {
    override fun execute(event: ButtonInteractionEvent) {
        try {
            getOrCreateMusicManager(event.guild!!.id).stop()
            event.jda.directAudioController.disconnect(event.guild!!)
            event.interaction.reply("Đã dọn sách hàng chờ và xin chào tạm biệt <3").queue()
        } catch (e: Exception) {
            event.interaction.reply("❌ | Có lỗi khi stop").queue()
        }
    }
}