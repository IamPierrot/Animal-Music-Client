package dev.pierrot.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun getLogger(name: String): Logger {
    return LoggerFactory.getLogger(name)
}

fun getLogger(clazz: Class<*>): Logger {
    return LoggerFactory.getLogger(clazz)
}