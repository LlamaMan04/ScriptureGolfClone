package com.llamaman04.scripturegolfclone.data

import android.content.res.AssetManager
import com.llamaman04.scripturegolfclone.domain.ScriptureBookContent
import com.llamaman04.scripturegolfclone.domain.ScriptureCatalogIndex
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ScriptureCatalogException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

data class CatalogBookKey(val workId: String, val bookId: String)

interface ScriptureCatalogDataSource {
    fun loadIndex(): ScriptureCatalogIndex
    fun loadBook(workId: String, bookId: String): ScriptureBookContent
}

class JsonScriptureCatalogDataSource(
    private val indexJson: String,
    private val bookJsonByKey: Map<CatalogBookKey, String>,
    private val json: Json = Json { ignoreUnknownKeys = false }
) : ScriptureCatalogDataSource {
    override fun loadIndex(): ScriptureCatalogIndex = decode("catalog index") {
        json.decodeFromString(ScriptureCatalogIndex.serializer(), indexJson)
    }

    override fun loadBook(workId: String, bookId: String): ScriptureBookContent {
        val key = CatalogBookKey(workId, bookId)
        val bookJson = bookJsonByKey[key]
            ?: throw ScriptureCatalogException("No catalog document exists for $workId/$bookId.")
        return decode("catalog document for $workId/$bookId") {
            json.decodeFromString(ScriptureBookContent.serializer(), bookJson)
        }
    }

    private fun <T> decode(description: String, parse: () -> T): T = try {
        parse()
    } catch (exception: SerializationException) {
        throw ScriptureCatalogException("The $description is not valid JSON.", exception)
    }
}

class AssetScriptureCatalogDataSource(private val assets: AssetManager) : ScriptureCatalogDataSource {
    override fun loadIndex(): ScriptureCatalogIndex = JsonScriptureCatalogDataSource(readAsset("scriptures/index.json"), emptyMap()).loadIndex()

    override fun loadBook(workId: String, bookId: String): ScriptureBookContent {
        val key = CatalogBookKey(workId, bookId)
        val document = readAsset("scriptures/books/$workId/$bookId.json")
        return JsonScriptureCatalogDataSource("{}", mapOf(key to document)).loadBook(workId, bookId)
    }

    private fun readAsset(path: String): String = try {
        assets.open(path).bufferedReader().use { it.readText() }
    } catch (exception: Exception) {
        throw ScriptureCatalogException("The bundled $path could not be loaded.", exception)
    }
}
