package dev.pierrot.components.base

import dev.pierrot.service.getLogger
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import org.reflections.Reflections

class ComponentRegistry {
    private val components = mutableMapOf<String, BaseDiscordComponent<*>>()
    private val logger = getLogger(this::class.java)

    private fun register(component: BaseDiscordComponent<*>) {
        components[component.customId] = component
    }

    private fun getComponent(customId: String): BaseDiscordComponent<*>? =
        components[customId]

    private fun discoverComponents() {
        val packageName = "dev.pierrot.components.imps"
        try {
            val reflections = Reflections(packageName)
            val componentClasses = reflections.getSubTypesOf(BaseDiscordComponent::class.java)
                .filter { it.`package`.name.startsWith(packageName) }

            componentClasses.forEach { componentClass ->
                try {
                    val instance = componentClass.getDeclaredConstructor().newInstance()
                    register(instance)
                } catch (e: Exception) {
                    logger.error("Failed to instantiate component: ${componentClass.simpleName}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Component discovery failed", e)
        }
    }


    fun onHandleComponent(event: GenericComponentInteractionCreateEvent) {
        val customId = event.componentId
        val component = getComponent(customId) ?: return

        try {
            @Suppress("UNCHECKED_CAST")
            (component as BaseDiscordComponent<GenericComponentInteractionCreateEvent>)
                .handleInteraction(event)
        } catch (e: Exception) {
            logger.error("Error handling interaction for component: $customId", e)
            event.reply("An error occurred while processing your interaction.").queue()
        }
    }

    companion object {
        val instance by lazy { ComponentRegistry() }
    }

    init {
        discoverComponents()
    }
}
