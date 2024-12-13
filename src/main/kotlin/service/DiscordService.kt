package dev.pierrot.service

import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

fun embed(): EmbedBuilder {
    return EmbedBuilder()
        .setColor(Color.pink)
}
