package dev.pierrot.components.imps

import dev.pierrot.App
import dev.pierrot.components.core.ButtonComponent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class Pause : ButtonComponent(
    "pause",
    true
) {

    override fun execute(event: ButtonInteractionEvent) {
        App.ServiceLocator.lavalinkClient
            .getOrCreateLink(event.guild!!.idLong)
            .cachedPlayer?.let { player ->
                player.setPaused(!player.paused)
                    .subscribe {
                        event.reply("Player has been " + (if (it.paused) "paused" else "resumed") + "!").queue()
                    }
            }
    }
}