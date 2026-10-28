package com.llamaman04.scripturegolfclone.domain

object GameRules {
    fun isBookCorrect(verse: ScriptureVerse, bookId: String): Boolean =
        verse.bookId == bookId

    fun isChapterCorrect(verse: ScriptureVerse, chapter: Int): Boolean =
        verse.chapter == chapter

    fun incrementIncorrectGuesses(current: Int): Int = current + 1
}
