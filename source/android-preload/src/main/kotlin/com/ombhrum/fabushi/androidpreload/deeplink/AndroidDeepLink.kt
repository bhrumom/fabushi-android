package com.ombhrum.fabushi.androidpreload.deeplink

internal sealed interface AndroidDeepLink {
    data class AuthCompletion(
        val attemptId: String,
        val status: AuthCompletionStatus,
    ) : AndroidDeepLink

    data class Agent(val agentId: String) : AndroidDeepLink

    data class AppSection(val section: String) : AndroidDeepLink
}

internal enum class AuthCompletionStatus {
    COMPLETED,
    CANCELLED,
    FAILED,
}
