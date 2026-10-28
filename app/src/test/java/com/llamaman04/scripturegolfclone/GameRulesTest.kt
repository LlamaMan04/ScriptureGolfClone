package com.llamaman04.scripturegolfclone

import com.llamaman04.scripturegolfclone.domain.GameRules
import com.llamaman04.scripturegolfclone.domain.ScriptureVerse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {
    private val verse = ScriptureVerse("alma", "Alma", 37, 6, "Text")

    @Test
    fun bookGuessIsComparedByCanonicalId() {
        assertTrue(GameRules.isBookCorrect(verse, "alma"))
        assertFalse(GameRules.isBookCorrect(verse, "mosiah"))
    }

    @Test
    fun chapterGuessMatchesVerseChapter() {
        assertTrue(GameRules.isChapterCorrect(verse, 37))
        assertFalse(GameRules.isChapterCorrect(verse, 36))
    }

    @Test
    fun incorrectGuessAddsOnePoint() {
        assertEquals(3, GameRules.incrementIncorrectGuesses(2))
    }
}
