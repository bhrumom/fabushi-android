package com.ombhrum.fabushi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidVersionTest {
    @Test
    fun newerSemanticVersionWinsEvenWithLowerBuildCode() {
        assertTrue(AndroidVersion.isNewer("1.0.5", 1, "1.0.4", 999_999))
        assertFalse(AndroidVersion.isNewer("1.0.3", 2_000_000, "1.0.4", 1))
    }

    @Test
    fun sameVersionUsesMonotonicAndroidVersionCode() {
        assertTrue(AndroidVersion.isNewer("1.0.4", 101, "1.0.4", 100))
        assertFalse(AndroidVersion.isNewer("1.0.4", 100, "1.0.4", 100))
    }

    @Test
    fun stableVersionSortsAfterPrerelease() {
        assertTrue(AndroidVersion.compare("2.0.0", "2.0.0-beta.4") > 0)
        assertTrue(AndroidVersion.compare("2.0.0-beta.10", "2.0.0-beta.2") > 0)
    }
}
