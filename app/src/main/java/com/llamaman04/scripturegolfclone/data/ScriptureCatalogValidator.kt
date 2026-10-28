package com.llamaman04.scripturegolfclone.data

import com.llamaman04.scripturegolfclone.domain.ScriptureBookContent
import com.llamaman04.scripturegolfclone.domain.ScriptureCatalogIndex

object ScriptureCatalogValidator {
    fun validateIndex(index: ScriptureCatalogIndex) {
        val issues = buildList {
            if (index.version <= 0) add("Catalog version must be positive.")
            if (index.works.isEmpty()) add("Catalog must contain at least one work.")
            duplicates(index.works.map { it.id }, "work IDs", this)
            index.works.forEach { work ->
                if (work.id.isBlank()) add("Catalog contains a work with no ID.")
                if (work.displayName.isBlank()) add("Work '${work.id}' has no display name.")
                if (work.books.isEmpty()) add("Work '${work.id}' has no books.")
                duplicates(work.books.map { it.id }, "book IDs in '${work.id}'", this)
                work.books.forEach { book ->
                    if (book.displayName.isBlank()) add("Book '${book.id}' has no display name.")
                    if (book.chapterCount <= 0) add("Book '${book.id}' has an invalid chapter count.")
                    if (book.chapters.isEmpty()) add("Book '${book.id}' has no indexed chapters.")
                    duplicates(book.chapters.map { it.number }, "chapter numbers in '${book.id}'", this)
                    book.chapters.forEach { chapter ->
                        if (chapter.number <= 0 || chapter.number > book.chapterCount) add("Book '${book.id}' has an invalid indexed chapter number.")
                        if (chapter.verseCount <= 0) add("Book '${book.id}', chapter ${chapter.number} has no indexed verses.")
                    }
                }
            }
            duplicates(index.works.flatMap { it.books.map { book -> book.id } }, "catalog book IDs", this)
        }
        throwWhenInvalid(issues)
    }

    fun validateBookContent(index: ScriptureCatalogIndex, content: ScriptureBookContent) {
        val book = index.works.firstOrNull { it.id == content.workId }?.books?.firstOrNull { it.id == content.bookId }
        val issues = buildList {
            if (book == null) add("Catalog document references an unknown book '${content.workId}/${content.bookId}'.")
            if (book != null) {
                duplicates(content.chapters.map { it.number }, "chapter numbers in '${content.bookId}'", this)
                if (content.chapters.map { it.number }.toSet() != book.chapters.map { it.number }.toSet()) add("Catalog document chapters do not match its index for '${content.bookId}'.")
                content.chapters.forEach { chapter ->
                    val indexedChapter = book.chapters.firstOrNull { it.number == chapter.number }
                    if (chapter.verses.isEmpty()) add("Book '${content.bookId}', chapter ${chapter.number} has no verses.")
                    if (indexedChapter != null && indexedChapter.verseCount != chapter.verses.size) add("Book '${content.bookId}', chapter ${chapter.number} verse count does not match its index.")
                    duplicates(chapter.verses.map { it.number }, "verse numbers in '${content.bookId}' chapter ${chapter.number}", this)
                    chapter.verses.forEach { verse ->
                        if (verse.number <= 0) add("Book '${content.bookId}' has an invalid verse number.")
                        if (verse.text.isBlank()) add("Book '${content.bookId}', verse ${verse.number} has no text.")
                    }
                }
            }
        }
        throwWhenInvalid(issues)
    }

    private fun duplicates(values: List<Any>, label: String, issues: MutableList<String>) {
        val duplicates = values.groupBy { it }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) issues.add("Duplicate $label: ${duplicates.joinToString()}.")
    }

    private fun throwWhenInvalid(issues: List<String>) {
        if (issues.isNotEmpty()) throw ScriptureCatalogException(issues.joinToString(separator = " "))
    }
}
