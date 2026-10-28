package com.llamaman04.scripturegolfclone

import com.llamaman04.scripturegolfclone.data.ScriptureRepository
import com.llamaman04.scripturegolfclone.domain.GameUiState
import com.llamaman04.scripturegolfclone.domain.ScriptureBook
import com.llamaman04.scripturegolfclone.domain.ScriptureVerse
import com.llamaman04.scripturegolfclone.ui.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {
    private val verse = ScriptureVerse("alma", "Alma", 37, 6, "Text")
    private val repository = object : ScriptureRepository {
        override val books = listOf(
            ScriptureBook("mosiah", "Mosiah", 29),
            ScriptureBook("alma", "Alma", 63)
        )

        override fun randomVerse(previous: ScriptureVerse?) = verse
    }

    @Test
    fun bookMustBeCorrectBeforeChapterIsAvailable() {
        val viewModel = GameViewModel(repository)

        viewModel.selectBook("mosiah")
        viewModel.submitBookGuess()

        val afterWrongBook = viewModel.uiState.value as GameUiState.GuessingBook
        assertEquals(1, afterWrongBook.incorrectGuesses)

        viewModel.selectBook("alma")
        viewModel.submitBookGuess()
        assertTrue(viewModel.uiState.value is GameUiState.GuessingChapter)

        viewModel.submitChapterGuess(36)
        val afterWrongChapter = viewModel.uiState.value as GameUiState.GuessingChapter
        assertEquals(2, afterWrongChapter.incorrectGuesses)

        viewModel.submitChapterGuess(37)
        assertTrue(viewModel.uiState.value is GameUiState.RoundComplete)
        assertEquals(2, (viewModel.uiState.value as GameUiState.RoundComplete).result.incorrectGuesses)
    }
}
