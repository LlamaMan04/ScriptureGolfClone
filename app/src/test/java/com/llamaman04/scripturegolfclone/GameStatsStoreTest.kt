package com.llamaman04.scripturegolfclone

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.llamaman04.scripturegolfclone.data.DataStoreGameStatsStore
import com.llamaman04.scripturegolfclone.domain.GameSettings
import com.llamaman04.scripturegolfclone.domain.RoundHistoryEntry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameStatsStoreTest {
    @Test
    fun savesSettingsAndCompletedRoundHistory() = runBlocking {
        val file = File.createTempFile("scripture-golf-stats", ".preferences_pb")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = DataStoreGameStatsStore(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        )

        try {
            store.saveSettings(GameSettings(setOf("new-testament"), 5))
            store.recordCompletedRound(
                RoundHistoryEntry("john", "John", 3, 16, 2, 1234L)
            )

            val stats = store.stats.first()
            assertEquals(setOf("new-testament"), stats.settings.selectedWorkIds)
            assertEquals(5, stats.settings.roundLength)
            assertEquals(1, stats.completedRounds)
            assertEquals(2, stats.totalIncorrectGuesses)
            assertEquals(2, stats.bestRoundScore)
            assertEquals(1, stats.currentStreak)
            assertEquals(1, stats.bestStreak)
            assertEquals(listOf("john"), stats.recentRounds.map { it.bookId })
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    @Test
    fun emptyStoreUsesDefaultStats() = runBlocking {
        val file = File.createTempFile("scripture-golf-defaults", ".preferences_pb")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = DataStoreGameStatsStore(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        )

        try {
            val stats = store.stats.first()
            assertEquals(10, stats.settings.roundLength)
            assertEquals(0, stats.completedRounds)
            assertNull(stats.bestRoundScore)
            assertEquals(emptyList<Any>(), stats.recentRounds)
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    @Test
    fun clearHistoryRemovesRoundsButKeepsSettings() = runBlocking {
        val file = File.createTempFile("scripture-golf-clear", ".preferences_pb")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = DataStoreGameStatsStore(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        )

        try {
            store.saveSettings(GameSettings(setOf("book-of-mormon"), 20))
            store.recordCompletedRound(RoundHistoryEntry("alma", "Alma", 37, 6, 1, 1234L))
            store.clearHistory()

            val stats = store.stats.first()
            assertEquals(setOf("book-of-mormon"), stats.settings.selectedWorkIds)
            assertEquals(20, stats.settings.roundLength)
            assertEquals(0, stats.completedRounds)
            assertEquals(0, stats.totalIncorrectGuesses)
            assertNull(stats.bestRoundScore)
            assertEquals(0, stats.bestStreak)
            assertEquals(emptyList<Any>(), stats.recentRounds)
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
