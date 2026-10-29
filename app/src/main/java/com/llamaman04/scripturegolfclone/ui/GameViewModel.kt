package com.llamaman04.scripturegolfclone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamaman04.scripturegolfclone.data.ScriptureCatalogException
import com.llamaman04.scripturegolfclone.data.ScriptureRepository
import com.llamaman04.scripturegolfclone.data.GameStatsStore
import com.llamaman04.scripturegolfclone.domain.GameRules
import com.llamaman04.scripturegolfclone.domain.GameSettings
import com.llamaman04.scripturegolfclone.domain.GameStats
import com.llamaman04.scripturegolfclone.domain.GameUiState
import com.llamaman04.scripturegolfclone.domain.RoundHistoryEntry
import com.llamaman04.scripturegolfclone.domain.RoundResult
import com.llamaman04.scripturegolfclone.domain.ScriptureBook
import com.llamaman04.scripturegolfclone.domain.ScriptureVerse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: ScriptureRepository,
    private val statsStore: GameStatsStore? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private var loadedBooks = emptyList<ScriptureBook>()
    val works get() = repository.works
    val books: List<ScriptureBook>
        get() = loadedBooks

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentVerse: ScriptureVerse? = null
    private var incorrectGuesses = 0
    private var incorrectBookGuesses = emptyList<String>()
    private var incorrectChapterGuesses = emptyList<Int>()
    private var roundNumber = 1
    private var selectedWorkIds = emptySet<String>()
    private var roundLength = 10
    private var sessionRound = 0
    private var sessionScore = 0
    private var hasUserChangedSettings = false
    private var latestStats = GameStats()

    init {
        loadLanding()
        statsStore?.let { store ->
            viewModelScope.launch {
                store.stats.collect { stats ->
                    latestStats = stats
                    if (_uiState.value is GameUiState.History) {
                        _uiState.value = GameUiState.History(stats)
                    }
                    applySavedSettings(stats.settings)
                }
            }
        }
    }

    fun openHome() {
        loadHome()
    }

    fun openHistory() {
        _uiState.value = GameUiState.History(latestStats)
    }

    fun closeHistory() {
        loadHome()
    }

    fun clearHistory() {
        latestStats = latestStats.copy(
            completedRounds = 0,
            totalIncorrectGuesses = 0,
            bestRoundScore = null,
            currentStreak = 0,
            bestStreak = 0,
            recentRounds = emptyList()
        )
        if (_uiState.value is GameUiState.History) {
            _uiState.value = GameUiState.History(latestStats)
        }
        statsStore?.let { store ->
            viewModelScope.launch { store.clearHistory() }
        }
    }

    fun toggleWork(workId: String) {
        val state = _uiState.value as? GameUiState.Home ?: return
        hasUserChangedSettings = true
        val updated = if (workId in state.selectedWorkIds) state.selectedWorkIds - workId
        else state.selectedWorkIds + workId
        selectedWorkIds = updated
        _uiState.value = state.copy(selectedWorkIds = updated)
        saveSettings(updated, state.roundLength)
    }

    fun toggleAllWorks() {
        val state = _uiState.value as? GameUiState.Home ?: return
        hasUserChangedSettings = true
        val updated = if (state.selectedWorkIds.size == state.works.size) emptySet() else state.works.map { it.id }.toSet()
        selectedWorkIds = updated
        _uiState.value = state.copy(selectedWorkIds = updated)
        saveSettings(updated, state.roundLength)
    }

    fun setRoundLength(length: Int) {
        val state = _uiState.value as? GameUiState.Home ?: return
        hasUserChangedSettings = true
        roundLength = length
        _uiState.value = state.copy(roundLength = length)
        saveSettings(state.selectedWorkIds, length)
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
        if (_uiState.value is GameUiState.Home || _uiState.value is GameUiState.Landing) {
            sessionRound = 1
            sessionScore = 0
            if (selectedWorkIds.isEmpty()) {
                selectedWorkIds = repository.works.map { it.id }.toSet()
            }
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
            incorrectBookGuesses = incorrectBookGuesses + selectedBookId
            _uiState.value = state.copy(
                incorrectGuesses = incorrectGuesses,
                feedback = "Not quite. Try another book.",
                incorrectBookGuesses = incorrectBookGuesses
            )
        }
    }

    fun submitChapterGuess(chapter: Int?) {
        val state = _uiState.value as? GameUiState.GuessingChapter ?: return
        val guessedChapter = chapter ?: return
        val verse = state.verse

        if (GameRules.isChapterCorrect(verse, guessedChapter)) {
            sessionScore += incorrectGuesses
            recordCompletedRound(verse, incorrectGuesses)
            _uiState.value = GameUiState.RoundComplete(
                result = RoundResult(verse, incorrectGuesses),
                roundNumber = roundNumber,
                roundScore = sessionScore,
                roundLength = roundLength,
                isRoundFinished = sessionRound >= roundLength
            )
        } else {
            incorrectGuesses = GameRules.incrementIncorrectGuesses(incorrectGuesses)
            val direction = if (guessedChapter < verse.chapter) "higher" else "lower"
            incorrectChapterGuesses = incorrectChapterGuesses + guessedChapter
            _uiState.value = state.copy(
                incorrectGuesses = incorrectGuesses,
                feedback = "Not quite. Try a $direction chapter.",
                incorrectChapterGuesses = incorrectChapterGuesses
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
            loadedBooks = booksForSelectedWorks()
            val verse = repository.randomVerse(selectedWorkIds, currentVerse)
            currentVerse = verse
            incorrectGuesses = 0
            incorrectBookGuesses = emptyList()
            incorrectChapterGuesses = emptyList()
            _uiState.value = GameUiState.GuessingBook(
                verse = verse,
                roundNumber = roundNumber
            )
        } catch (exception: ScriptureCatalogException) {
            _uiState.value = GameUiState.Error(exception.message ?: "Unable to load the scripture catalog.")
        }
    }

    private fun booksForSelectedWorks(): List<ScriptureBook> {
        val selectedWorkNames = repository.works
            .filter { it.id in selectedWorkIds }
            .map { it.displayName }
            .toSet()
        return repository.books.filter { it.work in selectedWorkNames }
    }

    private fun loadHome() {
        try {
            loadedBooks = repository.books
            _uiState.value = GameUiState.Home(repository.works, selectedWorkIds, roundLength)
        } catch (exception: ScriptureCatalogException) {
            _uiState.value = GameUiState.Error(exception.message ?: "Unable to load the scripture catalog.")
        }
    }

    private fun loadLanding() {
        try {
            loadedBooks = repository.books
            if (selectedWorkIds.isEmpty()) {
                selectedWorkIds = repository.works.map { it.id }.toSet()
            }
            _uiState.value = GameUiState.Landing
        } catch (exception: ScriptureCatalogException) {
            _uiState.value = GameUiState.Error(exception.message ?: "Unable to load the scripture catalog.")
        }
    }

    private fun applySavedSettings(settings: GameSettings) {
        val state = _uiState.value
        if (hasUserChangedSettings || (state !is GameUiState.Landing && state !is GameUiState.Home)) return

        val availableWorkIds = repository.works.map { it.id }.toSet()
        val restoredWorkIds = settings.selectedWorkIds.intersect(availableWorkIds)
            .ifEmpty { availableWorkIds }
        val restoredRoundLength = settings.roundLength.takeIf { it in ROUND_LENGTH_OPTIONS }
            ?: DEFAULT_ROUND_LENGTH

        selectedWorkIds = restoredWorkIds
        roundLength = restoredRoundLength
        if (state is GameUiState.Home) {
            _uiState.value = state.copy(
                selectedWorkIds = restoredWorkIds,
                roundLength = restoredRoundLength
            )
        }
    }

    private fun saveSettings(workIds: Set<String>, length: Int) {
        statsStore ?: return
        viewModelScope.launch {
            statsStore.saveSettings(GameSettings(workIds, length))
        }
    }

    private fun recordCompletedRound(verse: ScriptureVerse, score: Int) {
        statsStore ?: return
        viewModelScope.launch {
            statsStore.recordCompletedRound(
                RoundHistoryEntry(
                    bookId = verse.bookId,
                    bookName = verse.bookName,
                    chapter = verse.chapter,
                    verse = verse.verse,
                    incorrectGuesses = score,
                    completedAtEpochMillis = clock()
                )
            )
        }
    }

    private companion object {
        val ROUND_LENGTH_OPTIONS = setOf(1, 5, 10, 20)
        const val DEFAULT_ROUND_LENGTH = 10
    }
}
