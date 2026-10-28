package com.llamaman04.scripturegolfclone

import com.llamaman04.scripturegolfclone.data.CatalogBookKey
import com.llamaman04.scripturegolfclone.data.CatalogScriptureRepository
import com.llamaman04.scripturegolfclone.data.JsonScriptureCatalogDataSource
import com.llamaman04.scripturegolfclone.data.ScriptureCatalogException
import com.llamaman04.scripturegolfclone.data.ScriptureCatalogValidator
import com.llamaman04.scripturegolfclone.domain.ScriptureBookContent
import com.llamaman04.scripturegolfclone.domain.ScriptureCatalogIndex
import com.llamaman04.scripturegolfclone.domain.ScriptureCatalogVerse
import com.llamaman04.scripturegolfclone.domain.ScriptureChapter
import com.llamaman04.scripturegolfclone.domain.ScriptureChapterIndex
import com.llamaman04.scripturegolfclone.domain.ScriptureWork
import com.llamaman04.scripturegolfclone.domain.ScriptureBookIndex
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScriptureCatalogTest {
    @Test
    fun indexSelectsBookMetadataBeforeLoadingItsDocument() {
        val repository = CatalogScriptureRepository(
            JsonScriptureCatalogDataSource(
                validIndexJson,
                mapOf(CatalogBookKey("book-of-mormon", "alma") to validBookJson)
            ),
            Random(0)
        )

        assertEquals(listOf("alma"), repository.books.map { it.id })
        assertEquals("Book of Mormon", repository.books.single().work)
        assertEquals(listOf(37), repository.chaptersForBook("alma"))

        val verse = repository.randomVerse()
        assertEquals("alma", verse.bookId)
        assertEquals(37, verse.chapter)
        assertEquals(6, verse.verse)
    }

    @Test
    fun validatorRejectsBookDocumentWithIncorrectVerseCount() {
        val index = ScriptureCatalogIndex(
            version = 1,
            works = listOf(ScriptureWork("book-of-mormon", "Book of Mormon", listOf(
                ScriptureBookIndex("alma", "Alma", 63, listOf(ScriptureChapterIndex(37, 1)))
            )))
        )
        val invalidContent = ScriptureBookContent(
            "book-of-mormon",
            "alma",
            listOf(ScriptureChapter(37, listOf(
                ScriptureCatalogVerse(6, "One"),
                ScriptureCatalogVerse(7, "Two")
            )))
        )

        assertThrows(ScriptureCatalogException::class.java) {
            ScriptureCatalogValidator.validateBookContent(index, invalidContent)
        }
    }

    @Test
    fun parserRejectsIndexWithoutRequiredMetadata() {
        assertThrows(ScriptureCatalogException::class.java) {
            JsonScriptureCatalogDataSource("""{"version": 1}""", emptyMap()).loadIndex()
        }
    }

    private companion object {
        val validIndexJson = """
            {
              "version": 1,
              "works": [{
                "id": "book-of-mormon",
                "displayName": "Book of Mormon",
                "books": [{
                  "id": "alma",
                  "displayName": "Alma",
                  "chapterCount": 63,
                  "chapters": [{ "number": 37, "verseCount": 1 }]
                }]
              }]
            }
        """.trimIndent()

        val validBookJson = """
            {
              "workId": "book-of-mormon",
              "bookId": "alma",
              "chapters": [{
                "number": 37,
                "verses": [{ "number": 6, "text": "By small and simple things." }]
              }]
            }
        """.trimIndent()
    }
}
