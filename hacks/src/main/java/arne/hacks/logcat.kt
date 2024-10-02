package arne.hacks

import logcat.LogPriority
import logcat.logcat as squareLogcat

inline fun Any.logcat(
    priority: LogPriority = LogPriority.DEBUG,
    tag: String? = null,
    message: () -> String
) {
    squareLogcat(priority = priority, tag = tag, message = message)
}

inline fun Any.logcat(
    tag: String,
    priority: LogPriority? = LogPriority.DEBUG,
    message: () -> String
) {
    squareLogcat(priority = priority!!, tag = tag, message = message)
}

