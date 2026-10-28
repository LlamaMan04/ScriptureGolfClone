package com.llamaman04.scripturegolfclone.domain

data class ScriptureVerse(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class ScriptureBook(
    val id: String,
    val displayName: String,
    val chapterCount: Int
)

data class RoundResult(
    val verse: ScriptureVerse,
    val incorrectGuesses: Int
)

sealed interface GameUiState {
    data object Loading : GameUiState

    data class GuessingBook(
        val verse: ScriptureVerse,
        val selectedBookId: String? = null,
        val incorrectGuesses: Int = 0,
        val roundNumber: Int = 1,
        val feedback: String? = null
    ) : GameUiState

    data class GuessingChapter(
        val verse: ScriptureVerse,
        val selectedChapter: Int? = null,
        val incorrectGuesses: Int,
        val roundNumber: Int,
        val feedback: String? = null
    ) : GameUiState

    data class RoundComplete(
        val result: RoundResult,
        val roundNumber: Int
    ) : GameUiState
}
