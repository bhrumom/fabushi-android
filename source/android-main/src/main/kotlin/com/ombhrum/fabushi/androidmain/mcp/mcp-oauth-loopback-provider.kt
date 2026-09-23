package com.ombhrum.fabushi.androidmain.mcp

import com.ombhrum.fabushi.androidpreload.deeplink.AndroidDeepLink
import com.ombhrum.fabushi.androidpreload.runtime.AndroidCoordinatorPort
import com.ombhrum.fabushi.androidpreload.runtime.AndroidMcpOAuthCompletion
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal data class AndroidMcpOAuthAuthorization(
    val authorizationUrl: String,
    val provider: String,
    val state: String,
)

internal class AndroidMcpOAuthLoopbackProvider(
    private val coordinator: AndroidCoordinatorPort,
) {
    fun registerAuthorization(
        authorizationUrl: String,
        provider: String,
    ): AndroidMcpOAuthAuthorization? {
        val parsed = parseAuthorization(authorizationUrl, provider) ?: return null
        return if (coordinator.mcpOAuthRegister(parsed.state, parsed.provider)) parsed else null
    }

    fun complete(
        callback: AndroidDeepLink.McpOAuthCallback,
    ): AndroidMcpOAuthCompletion =
        coordinator.mcpOAuthComplete(
            state = callback.state,
            code = callback.code,
            error = callback.error,
        )

    fun failRegisteredAuthorization(
        state: String,
        reason: String,
    ): AndroidMcpOAuthCompletion =
        coordinator.mcpOAuthComplete(
            state = state,
            code = null,
            error = reason,
        )

    companion object {
        internal const val CALLBACK_URL = "fabushi://mcp-oauth/callback"
        private const val MAX_URL_LENGTH = 16_384
        private val statePattern = Regex("^[A-Za-z0-9._~-]{16,512}$")

        internal fun parseAuthorization(
            authorizationUrl: String,
            provider: String,
        ): AndroidMcpOAuthAuthorization? {
            if (authorizationUrl.length !in 1..MAX_URL_LENGTH) return null
            val normalizedProvider = provider.trim()
            if (normalizedProvider.isEmpty() || normalizedProvider.length > 200) return null

            val uri = runCatching { URI(authorizationUrl) }.getOrNull() ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true)) return null
            if (uri.host.isNullOrBlank() || uri.userInfo != null || uri.fragment != null) return null

            val query = parseQuery(uri.rawQuery) ?: return null
            val redirects = query["redirect_uri"].orEmpty()
            val states = query["state"].orEmpty()
            if (redirects.size != 1 || states.size != 1) return null

            val redirect = runCatching { URI(redirects.single()) }.getOrNull() ?: return null
            if (!redirect.scheme.equals("fabushi", ignoreCase = true)) return null
            if (!redirect.host.equals("mcp-oauth", ignoreCase = true)) return null
            if (redirect.path != "/callback") return null
            if (redirect.userInfo != null || redirect.port != -1 || redirect.fragment != null) return null
            if (!redirect.rawQuery.isNullOrEmpty()) return null

            val state = states.single()
            if (!statePattern.matches(state)) return null

            return AndroidMcpOAuthAuthorization(
                authorizationUrl = uri.toASCIIString(),
                provider = normalizedProvider,
                state = state,
            )
        }

        private fun parseQuery(raw: String?): Map<String, List<String>>? {
            if (raw.isNullOrBlank()) return emptyMap()
            val out = linkedMapOf<String, MutableList<String>>()
            for (part in raw.split('&')) {
                if (part.isEmpty()) return null
                val separator = part.indexOf('=')
                val key = decode(if (separator < 0) part else part.substring(0, separator)) ?: return null
                val value = decode(if (separator < 0) "" else part.substring(separator + 1)) ?: return null
                out.getOrPut(key) { mutableListOf() }.add(value)
            }
            return out
        }

        private fun decode(raw: String): String? =
            runCatching {
                URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
            }.getOrNull()
    }
}
