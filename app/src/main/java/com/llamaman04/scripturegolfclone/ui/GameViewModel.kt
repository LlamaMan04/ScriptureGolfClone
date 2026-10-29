package com.llamaman04.scripturegolfclone.ui

import androidx.lifecycle.ViewModel
import com.llamaman04.scripturegolfclone.data.ScriptureCatalogException
import com.llamaman04.scripturegolfclone.data.ScriptureRepository
import com.llamaman04.scripturegolfclone.domain.GameRules
import com.llamaman04.scripturegolfclone.domain.GameUiState
import com.llamaman04.scripturegolfclone.domain.RoundResult
import com.llamaman04.scripturegolfclone.domain.ScriptureBook
import com.llamaman04.scripturegolfclone.domain.ScriptureVerse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(
    private val repository: ScriptureRepository
) : ViewModel() {
    private var loadedBooks = emptyList<ScriptureBook>()
    val works get() = repository.works
    val books: List<ScriptureBook>
        get() = loadedBooks

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentVerse: ScriptureVerse? = null
    private var incorrectGuesses = 0
    private var roundNumber = 1
    private var selectedWorkIds = repository.works.map { it.id }.toSet()
    private var roundLength = 10
    private var sessionRound = 0
    private var sessionScore = 0

    init {
        loadHome()
    }

    fun toggleWork(workId: String) {
        val state = _uiState.value as? GameUiState.Home ?: return
        val updated = if (workId in state.selectedWorkIds) state.selectedWorkIds - workId
        else state.selectedWorkIds + workId
        selectedWorkIds = updated
        _uiState.value = state.copy(selectedWorkIds = updated)
    }

    fun toggleAllWorks() {
        val state = _uiState.value as? GameUiState.Home ?: return
        val updated = if (state.selectedWorkIds.size == state.works.size) emptySet() else state.works.map { it.id }.toSet()
        selectedWorkIds = updated
        _uiState.value = state.copy(selectedWorkIds = updated)
    }

    fun setRoundLength(length: Int) {
        val state = _uiState.value as? GameUiState.Home ?: return
        roundLength = length
        _uiState.value = state.copy(roundLength = length)
    }

    fun startConfiguredRound() {
        val state = _uiState.value as? GameUiState.Home ?: return
        if (state.selectedWorkIds.isEmpty()) return
        selectedWorkIds = state.selectedWorkIds
        roundLength = state.roundLength
        roundNumber = 1
        sessionRound = 1
        sessionScore = 0
        startRound()
    }

    fun selectBook(bookId: String) {
        if (_uiState.value is GameUiState.Home) {
            sessionRound = 1
            sessionScore = 0
            startRound()
        }
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
            sessionScore += incorrectGuesses
            _uiState.value = GameUiState.RoundComplete(
                result = RoundResult(verse, incorrectGuesses),
                roundNumber = roundNumber,
                roundScore = sessionScore,
                roundLength = roundLength,
                isRoundFinished = sessionRound >= roundLength
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
        if (sessionRound >= roundLength) {
            loadHome()
            return
        }
        sessionRound++
        roundNumber++
        startRound()
    }

    fun exitRound() {
        loadHome()
    }

    fun retryLoading() {
        _uiState.value = GameUiState.Loading
        startRound()
    }

    private fun startRound() {
        try {
            loadedBooks = repository.books
            val verse = repository.randomVerse(selectedWorkIds, currentVerse)
            currentVerse = verse
            incorrectGuesses = 0
            _uiState.value = GameUiState.GuessingBook(
                verse = verse,
                roundNumber = roundNumber
            )
        } catch (exception: ScriptureCatalogException) {
            _uiState.value = GameUiState.Error(exception.message ?: "Unable to load the scripture catalog.")
        }
    }

    private fun loadHome() {
        try {
            loadedBooks = repository.books
            _uiState.value = GameUiState.Home(repository.works, selectedWorkIds, roundLength)
        } catch (exception: ScriptureCatalogException) {
            _uiState.value = GameUiState.Error(exception.message ?: "Unable to load the scripture catalog.")
        }
    }
}
