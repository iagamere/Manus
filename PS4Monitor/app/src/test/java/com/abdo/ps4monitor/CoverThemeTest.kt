package com.abdo.ps4monitor

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverThemeTest {
    @Test fun contrastMathIsWcagBounded() {
        assertEquals(21.0, ColorMath.contrast(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), .01)
        assertTrue(ColorMath.luminance(0xFFFFFFFF.toInt()) > ColorMath.luminance(0xFF000000.toInt()))
    }

    @Test fun analysisFindsSecondaryAndSpatialSignals() {
        val px = IntArray(64 * 64) { i -> if ((i / 64) < 32) 0xFFE53935.toInt() else 0xFF1565C0.toInt() }
        val a = CoverPalette.analyze(px)
        assertNotNull(a.secondary)
        assertTrue(a.contrast > 0f)
        assertTrue(a.confidence > 0f)
    }

    @Test fun emptyArtworkUsesSafeFallback() {
        val a = CoverPalette.analyze(IntArray(0))
        assertTrue(ColorMath.contrast(a.primary, 0xFF000000.toInt()) >= 1.0)
        assertEquals(0f, a.confidence, .001f)
    }

    @Test fun importedThemeIsValidatedAndSanitised() {
        val o = JSONObject().put("name", "Test").put("bg", "#ffffff").put("bg2", "#eeeeee").put("surface", "#dddddd")
            .put("accent", "#000000").put("accent2", "#111111").put("text", "#000000").put("text2", "#222222")
            .put("border", "#333333").put("progress", "#000000")
        val t = ThemeEngine.fromJson(o, keepId = false)
        assertNotNull(t)
        assertTrue(ColorMath.contrast(t!!.text, t.bg) >= 4.5)
    }
}
