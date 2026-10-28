package com.llamaman04.scripturegolfclone.data

import com.llamaman04.scripturegolfclone.domain.ScriptureBook
import com.llamaman04.scripturegolfclone.domain.ScriptureVerse
import kotlin.random.Random

interface ScriptureRepository {
    val books: List<ScriptureBook>
    fun randomVerse(previous: ScriptureVerse? = null): ScriptureVerse
}

class InMemoryScriptureRepository(
    private val random: Random = Random.Default
) : ScriptureRepository {
    private val verses = listOf(
        ScriptureVerse(
            bookId = "1-nephi",
            bookName = "1 Nephi",
            chapter = 1,
            verse = 1,
            text = "I, Nephi, having been born of goodly parents, therefore I was taught somewhat in all the learning of my father."
        ),
        ScriptureVerse(
            bookId = "mosiah",
            bookName = "Mosiah",
            chapter = 2,
            verse = 17,
            text = "And behold, I tell you these things that ye may learn wisdom; that ye may learn that when ye are in the service of your fellow beings ye are only in the service of your God."
        ),
        ScriptureVerse(
            bookId = "alma",
            bookName = "Alma",
            chapter = 37,
            verse = 6,
            text = "Now ye may suppose that this is foolishness in me; but behold I say unto you, that by small and simple things are great things brought to pass."
        )
    )

    override val books: List<ScriptureBook> = listOf(
        ScriptureBook("1-nephi", "1 Nephi", 22),
        ScriptureBook("mosiah", "Mosiah", 29),
        ScriptureBook("alma", "Alma", 63)
    )

    override fun randomVerse(previous: ScriptureVerse?): ScriptureVerse {
        val choices = verses.filterNot { it == previous }.ifEmpty { verses }
        return choices[random.nextInt(choices.size)]
    }
}
