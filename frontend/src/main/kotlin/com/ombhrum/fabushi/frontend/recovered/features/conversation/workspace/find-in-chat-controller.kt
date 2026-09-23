package com.ombhrum.fabushi

internal data class FindInChatMatch(
    val messageId: String,
    val occurrence: Int,
)

internal fun findInChatSearchText(message: ChatMessage): String =
    buildString {
        append(message.text)
        message.contactName?.let { append(' ').append(it) }
        message.pollQuestion?.let { append(' ').append(it) }
        message.pollOptions.forEach { append(' ').append(it.text) }
        message.mediaFileName?.let { append(' ').append(it) }
        message.forwardOrigin?.let { append(' ').append(it) }
    }.replace(Regex("\\s+"), " ").trim()

internal fun findInChatMatches(
    messages: List<ChatMessage>,
    query: String,
): List<FindInChatMatch> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return emptyList()
    return buildList {
        messages.forEach { message ->
            val text = findInChatSearchText(message).lowercase()
            var from = 0
            var occurrence = 0
            while (from <= text.length - needle.length) {
                val at = text.indexOf(needle, from)
                if (at < 0) break
                add(FindInChatMatch(message.id, occurrence++))
                from = at + needle.length.coerceAtLeast(1)
            }
        }
    }
}

internal fun stepFindInChatIndex(current: Int, delta: Int, count: Int): Int {
    if (count <= 0) return -1
    val base = current.takeIf { it in 0 until count } ?: 0
    return ((base + delta) % count + count) % count
}
