package com.llamaman04.scripturegolfclone.ui

import androidx.lifecycle.ViewModel
import com.llamaman04.scripturegolfclone.data.InMemoryScriptureRepository
import com.llamaman04.scripturegolfclone.data.ScriptureRepository
import com.llamaman04.scripturegolfclone.domain.GameRules
import com.llamaman04.scripturegolfclone.domain.GameUiState
import com.llamaman04.scripturegolfclone.domain.RoundResult
import com.llamaman04.scripturegolfclone.domain.ScriptureVerse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(
    private val repository: ScriptureRepository = InMemoryScriptureRepository()
) : ViewModel() {
    val books = repository.books

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentVerse: ScriptureVerse? = null
    private var incorrectGuesses = 0
    private var roundNumber = 1

    init {
        startRound()
    }

    fun selectBook(bookId: String) {
        val state = _uiState.value as? GameUiState.GuessingBook ?: return
        _uiState.value = state.copy(selectedBookId = bookId)
    }

    fun submitBookGuess() {
        val state = _uiState.value as? GameUiState.GuessingBook ?: return
        val selectedBookId = state.selectedBookId ?: return
        val verse = state.verse

        if (GameRules.isBookCorrect(verse, selectedBookId)) {
            _uiState.value = GameUiState.GuessingChapter(
                verse = verse,
                incorrectGuesses = incorrectGuesses,
                roundNumber = roundNumber,
                feedback = "Correct book! Now guess the chapter."
            )
        } else {
            incorrectGuesses = GameRules.incrementIncorrectGuesses(incorrectGuesses)
            _uiState.value = state.copy(
                incorrectGuesses = incorrectGuesses,
                feedback = "Not quite. Try another book."
            )
        }
    }

    fun submitChapterGuess(chapter: Int?) {
        val state = _uiState.value as? GameUiState.GuessingChapter ?: return
        val guessedChapter = chapter ?: return
        val verse = state.verse

        if (GameRules.isChapterCorrect(verse, guessedChapter)) {
            _uiState.value = GameUiState.RoundComplete(
                result = RoundResult(verse, incorrectGuesses),
                roundNumber = roundNumber
            )
        } else {
            incorrectGuesses = GameRules.incrementIncorrectGuesses(incorrectGuesses)
            _uiState.value = state.copy(
                incorrectGuesses = incorrectGuesses,
                feedback = "Not quite. Try another chapter."
            )
        }
    }

    fun nextRound() {
        roundNumber++
        startRound()
    }

    private fun startRound() {
        val verse = repository.randomVerse(currentVerse)
        currentVerse = verse
        incorrectGuesses = 0
        _uiState.value = GameUiState.GuessingBook(
            verse = verse,
            roundNumber = roundNumber
        )
    }
}
