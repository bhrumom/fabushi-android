package com.ombhrum.fabushi

internal fun committedAgentName(
    initialValue: String,
    draftValue: String,
): String? {
    val trimmed = draftValue.trim()
    return trimmed.takeIf { it.isNotEmpty() && it != initialValue }
}
