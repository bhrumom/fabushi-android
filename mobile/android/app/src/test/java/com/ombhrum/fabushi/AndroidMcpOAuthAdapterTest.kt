package com.ombhrum.fabushi

import com.ombhrum.fabushi.androidmain.mcp.AndroidMcpOAuthLoopbackProvider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidMcpOAuthAdapterTest {
    private fun authorization(
        redirect: String = AndroidMcpOAuthLoopbackProvider.CALLBACK_URL,
        state: String = "0123456789abcdef",
    ): String {
        val encoded = URLEncoder.encode(redirect, StandardCharsets.UTF_8.name())
        return "https://connector.example/authorize?client_id=abc&redirect_uri=$encoded&state=$state"
    }

    @Test
    fun authorizationRequiresHttpsExactAppCallbackAndBoundedState() {
        val parsed = AndroidMcpOAuthLoopbackProvider.parseAuthorization(
            authorization(),
            "github",
        )
        assertEquals("0123456789abcdef", parsed?.state)
        assertEquals("github", parsed?.provider)

        assertNull(
            AndroidMcpOAuthLoopbackProvider.parseAuthorization(
                authorization(redirect = "https://evil.example/callback"),
                "github",
            ),
        )
        assertNull(
            AndroidMcpOAuthLoopbackProvider.parseAuthorization(
                authorization(state = "short"),
                "github",
            ),
        )
        assertNull(
            AndroidMcpOAuthLoopbackProvider.parseAuthorization(
                "http://connector.example/authorize?redirect_uri=fabushi%3A%2F%2Fmcp-oauth%2Fcallback&state=0123456789abcdef",
                "github",
            ),
        )
    }

    @Test
    fun duplicateRedirectOrStateFailsClosed() {
        val redirect = URLEncoder.encode(
            AndroidMcpOAuthLoopbackProvider.CALLBACK_URL,
            StandardCharsets.UTF_8.name(),
        )
        assertNull(
            AndroidMcpOAuthLoopbackProvider.parseAuthorization(
                "https://connector.example/authorize?redirect_uri=$redirect&redirect_uri=$redirect&state=0123456789abcdef",
                "github",
            ),
        )
        assertNull(
            AndroidMcpOAuthLoopbackProvider.parseAuthorization(
                "https://connector.example/authorize?redirect_uri=$redirect&state=0123456789abcdef&state=fedcba9876543210",
                "github",
            ),
        )
    }
}
