package dev.pierrot.components.imps

import dev.pierrot.components.core.ButtonComponent
import dev.pierrot.service.getOrCreateMusicManager
import dev.pierrot.service.tempReply
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class Skip : ButtonComponent(
    "skip",
    true
) {

    override fun execute(event: ButtonInteractionEvent) {
        val guildMusicManager = getOrCreateMusicManager(event.guild!!.id)

        if (guildMusicManager.isPlaying()) {
            event.reply("Bỏ qua bài phát hiện tại!").queue()
            guildMusicManager.skip()
        }

        tempReply(event, "❌ | Không có bài nào đang phát")
    }

}