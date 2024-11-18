package dev.pierrot.commands.core

import java.time.Duration

// Cooldown Manager (Singleton Pattern)
class CooldownManager {
    private val cooldowns = mutableMapOf<String, Long>()

    fun getRemainingCooldown(key: String): Duration {
        val lastUse = cooldowns[key] ?: return Duration.ZERO
        val timePassed = System.currentTimeMillis() - lastUse
        val remainingTime = Duration.ofSeconds(2).toMillis() - timePassed
        return if (remainingTime > 0) Duration.ofMillis(remainingTime) else Duration.ZERO
    }

    fun applyCooldown(key: String, duration: Duration) {
        if (duration > Duration.ZERO) {
            cooldowns[key] = System.currentTimeMillis()
        }
    }
}

