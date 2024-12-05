package dev.pierrot.components.core

import dev.pierrot.components.base.BaseDiscordComponent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

abstract class ButtonComponent(
    customId: String,
    requireSameVoiceChannel: Boolean = false
) : BaseDiscordComponent<ButtonInteractionEvent>(customId, requireSameVoiceChannel) {

    abstract fun execute(event: ButtonInteractionEvent)

    override fun onInteract(event: ButtonInteractionEvent) {
        execute(event)
    }
}