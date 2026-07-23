package com.rahmanilab.lexico.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilsTest {

    @Test
    fun normalizeAnswer_trimsLowercasesAndCollapsesWhitespace() {
        assertEquals("give up", TextUtils.normalizeAnswer("  Give   Up "))
    }

    @Test
    fun answersMatch_isLenientAboutCaseAndSpacing() {
        assertTrue(TextUtils.answersMatch("Give Up", "  give   up "))
        assertFalse(TextUtils.answersMatch("give up", "give in"))
    }

    @Test
    fun clozeBlank_replacesTheWordCaseInsensitively() {
        val result = TextUtils.clozeBlank("Children are more Resilient than adults.", "resilient")
        assertTrue(result.contains("_____"))
        assertFalse(result.contains("Resilient"))
    }

    @Test
    fun clozeBlank_returnsOriginalWhenWordIsAbsent() {
        val sentence = "This sentence has no target."
        assertEquals(sentence, TextUtils.clozeBlank(sentence, "missing"))
    }
}
