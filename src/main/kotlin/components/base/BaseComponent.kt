package dev.pierrot.components.base

import dev.pierrot.service.getLogger
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.components.ComponentInteraction

abstract class BaseDiscordComponent<T : ComponentInteraction>(
    val customId: String,
    private val requireSameVoiceChannel: Boolean = false
) {
    protected val logger = getLogger(this::class.java)

    protected abstract fun onInteract(event: T)

    protected open fun validateVoiceState(
        member: Member,
        guild: Guild
    ): Boolean {
        if (!requireSameVoiceChannel) return true

        val memberVoiceChannel = member.voiceState?.channel
        val botVoiceChannel = guild.selfMember.voiceState?.channel

        return memberVoiceChannel != null &&
                memberVoiceChannel == botVoiceChannel
    }

    fun handleInteraction(event: T) {
        val member = event.member ?: run {
            logger.warn("Interaction from null member: $customId")
            return
        }

        val guild = event.guild ?: run {
            logger.warn("Interaction from null guild: $customId")
            return
        }

        if (!validateVoiceState(member, guild)) {
            event.deferEdit()
            return
        }

        onInteract(event)
    }
}
