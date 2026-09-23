package com.ombhrum.fabushi

import com.ombhrum.fabushi.androidmain.adapters.AndroidAccountOAuthAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidAccountOAuthAdapterTest {
    @Test
    fun externalAuthAcceptsOnlyHttpsWithoutCredentialsOrFragments() {
        val accepted = AndroidAccountOAuthAdapter.validateExternalAuthUrl(
            "https://auth.example.com/oauth?state=opaque",
        )
        assertEquals("https", accepted?.scheme)
        assertEquals("auth.example.com", accepted?.host)

        assertNull(AndroidAccountOAuthAdapter.validateExternalAuthUrl("http://auth.example.com"))
        assertNull(AndroidAccountOAuthAdapter.validateExternalAuthUrl("javascript:alert(1)"))
        assertNull(AndroidAccountOAuthAdapter.validateExternalAuthUrl("https://user:pass@auth.example.com"))
        assertNull(AndroidAccountOAuthAdapter.validateExternalAuthUrl("https://auth.example.com/#token"))
    }
}
