package com.ombhrum.fabushi.androidpreload.deeplink

sealed interface AndroidDeepLink {
    data class AuthCompletion(
        val attemptId: String,
        val status: AuthCompletionStatus,
    ) : AndroidDeepLink

    data class Agent(val agentId: String) : AndroidDeepLink

    data class AppSection(val section: String) : AndroidDeepLink
}

enum class AuthCompletionStatus {
    COMPLETED,
    CANCELLED,
    FAILED,
}
