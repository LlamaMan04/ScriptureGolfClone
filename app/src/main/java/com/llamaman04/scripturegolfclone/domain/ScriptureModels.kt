package com.llamaman04.scripturegolfclone.domain

import kotlinx.serialization.Serializable

@Serializable
data class ScriptureCatalogIndex(val version: Int, val works: List<ScriptureWork>)

@Serializable
data class ScriptureWork(val id: String, val displayName: String, val books: List<ScriptureBookIndex>)

data class ScriptureWorkOption(
    val id: String,
    val displayName: String,
    val bookCount: Int
)

@Serializable
data class ScriptureBookIndex(
    val id: String,
    val displayName: String,
    val chapterCount: Int,
    val chapters: List<ScriptureChapterIndex>
)

@Serializable
data class ScriptureChapterIndex(val number: Int, val verseCount: Int)

@Serializable
data class ScriptureBookContent(
    val workId: String,
    val bookId: String,
    val chapters: List<ScriptureChapter>
)

@Serializable
data class ScriptureChapter(val number: Int, val verses: List<ScriptureCatalogVerse>)

@Serializable
data class ScriptureCatalogVerse(val number: Int, val text: String)

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
    val chapterCount: Int,
    val work: String
)

data class RoundResult(val verse: ScriptureVerse, val incorrectGuesses: Int)

sealed interface GameUiState {
    data object Loading : GameUiState
    data object Landing : GameUiState
    data class Home(
        val works: List<ScriptureWorkOption>,
        val selectedWorkIds: Set<String> = works.map { it.id }.toSet(),
        val roundLength: Int = 10
    ) : GameUiState
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
        val roundNumber: Int,
        val roundScore: Int = result.incorrectGuesses,
        val roundLength: Int = 1,
        val isRoundFinished: Boolean = true
    ) : GameUiState
    data class Error(
        val message: String
    ) : GameUiState
}
