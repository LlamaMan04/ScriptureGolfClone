package com.llamaman04.scripturegolfclone.data

import com.llamaman04.scripturegolfclone.domain.ScriptureBook
import com.llamaman04.scripturegolfclone.domain.ScriptureBookContent
import com.llamaman04.scripturegolfclone.domain.ScriptureBookIndex
import com.llamaman04.scripturegolfclone.domain.ScriptureCatalogIndex
import com.llamaman04.scripturegolfclone.domain.ScriptureVerse
import com.llamaman04.scripturegolfclone.domain.ScriptureWork
import com.llamaman04.scripturegolfclone.domain.ScriptureWorkOption
import kotlin.random.Random

interface ScriptureRepository {
    val books: List<ScriptureBook>
    val works: List<ScriptureWorkOption>
        get() = books.groupBy { it.work }.map { (work, workBooks) ->
            ScriptureWorkOption(work.lowercase().replace(" ", "-"), work, workBooks.size)
        }
    fun chaptersForBook(bookId: String): List<Int>
    fun randomVerse(previous: ScriptureVerse? = null): ScriptureVerse
    fun randomVerse(includedWorkIds: Set<String>, previous: ScriptureVerse? = null): ScriptureVerse =
        randomVerse(previous)
}

class CatalogScriptureRepository(
    private val dataSource: ScriptureCatalogDataSource,
    private val random: Random = Random.Default
) : ScriptureRepository {
    private val index: ScriptureCatalogIndex by lazy {
        dataSource.loadIndex().also(ScriptureCatalogValidator::validateIndex)
    }

    private val indexedChapters by lazy {
        index.works.flatMap { work ->
            work.books.flatMap { book ->
                book.chapters.map { chapter -> IndexedChapter(work, book, chapter.number, chapter.verseCount) }
            }
        }
    }

    private var cachedBookKey: CatalogBookKey? = null
    private var cachedBook: ScriptureBookContent? = null

    override val books: List<ScriptureBook> by lazy {
        index.works.flatMap { work ->
            work.books.map { book ->
                ScriptureBook(book.id, book.displayName, book.chapterCount, work.displayName)
            }
        }
    }

    override val works: List<ScriptureWorkOption> by lazy {
        index.works.map { work ->
            ScriptureWorkOption(work.id, work.displayName, work.books.size)
        }
    }

    override fun chaptersForBook(bookId: String): List<Int> = index.works.asSequence()
        .flatMap { it.books.asSequence() }
        .firstOrNull { it.id == bookId }
        ?.chapters
        ?.map { it.number }
        .orEmpty()

    override fun randomVerse(previous: ScriptureVerse?): ScriptureVerse {
        return randomVerse(works.map { it.id }.toSet(), previous)
    }

    override fun randomVerse(includedWorkIds: Set<String>, previous: ScriptureVerse?): ScriptureVerse {
        val includedChapters = indexedChapters.filter { it.work.id in includedWorkIds }
        require(includedChapters.isNotEmpty()) { "At least one scripture work must be selected." }
        repeat(4) {
            val indexedChapter = selectIndexedChapter(includedChapters)
            val content = loadBook(indexedChapter.work.id, indexedChapter.book.id)
            val chapter = content.chapters.first { it.number == indexedChapter.chapterNumber }
            val selectedVerse = chapter.verses[random.nextInt(chapter.verses.size)]
            val verse = ScriptureVerse(
                bookId = indexedChapter.book.id,
                bookName = indexedChapter.book.displayName,
                chapter = chapter.number,
                verse = selectedVerse.number,
                text = selectedVerse.text
            )
            if (verse != previous || includedChapters.sumOf { it.verseCount } == 1) return verse
        }
        return randomVerse(includedWorkIds, null)
    }

    private fun selectIndexedChapter(chapters: List<IndexedChapter>): IndexedChapter {
        val totalVerseCount = chapters.sumOf { it.verseCount }
        var selection = random.nextInt(totalVerseCount)
        return chapters.first { chapter ->
            if (selection < chapter.verseCount) true else {
                selection -= chapter.verseCount
                false
            }
        }
    }

    private fun loadBook(workId: String, bookId: String): ScriptureBookContent {
        val key = CatalogBookKey(workId, bookId)
        if (cachedBookKey == key && cachedBook != null) return cachedBook!!
        return dataSource.loadBook(workId, bookId)
            .also { ScriptureCatalogValidator.validateBookContent(index, it) }
            .also {
                cachedBookKey = key
                cachedBook = it
            }
    }

    private data class IndexedChapter(
        val work: ScriptureWork,
        val book: ScriptureBookIndex,
        val chapterNumber: Int,
        val verseCount: Int
    )
}
