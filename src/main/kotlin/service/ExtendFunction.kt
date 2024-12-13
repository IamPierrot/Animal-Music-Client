package dev.pierrot.service

import java.util.*

fun String.toCapital(): String {
    if (this.isEmpty()) {
        return this
    }
    return this.substring(0, 1).uppercase(Locale.getDefault()) + this.substring(1)
}