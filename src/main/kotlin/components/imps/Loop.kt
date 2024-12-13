package dev.pierrot.components.imps

import dev.pierrot.components.core.ButtonComponent
import dev.pierrot.service.embed
import dev.pierrot.service.getOrCreateMusicManager
import dev.pierrot.service.tempReply
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import java.awt.Color

class Loop : ButtonComponent(
    "loop",
    true
) {
    override fun execute(event: ButtonInteractionEvent) {
        val guildMusicManager = getOrCreateMusicManager(event.guild!!.id)
        val methods = arrayOf("Lặp bài hát", "Lặp cả hàng chờ", "tắt vòng lặp")
        val loopMode = guildMusicManager.scheduler.getLoopMode()

        if (!guildMusicManager.isPlaying()) {
            tempReply(event.message, "❌ | Không có bài nào đang phát")
            return
        }

       guildMusicManager.scheduler.changeLoopMode(loopMode)

        val loopEmbed = embed()
            .setColor(Color.pink)
            .setDescription("Thiết lập chế độ : **${methods[loopMode]}** ✅")

        event.replyEmbeds(loopEmbed.build()).setEphemeral(true).queue()
    }
}