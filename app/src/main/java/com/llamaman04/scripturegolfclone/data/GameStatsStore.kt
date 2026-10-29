package com.llamaman04.scripturegolfclone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.llamaman04.scripturegolfclone.domain.GameSettings
import com.llamaman04.scripturegolfclone.domain.GameStats
import com.llamaman04.scripturegolfclone.domain.RoundHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException

private const val DATA_STORE_NAME = "game_stats"
private const val MAX_RECENT_ROUNDS = 50

private val Context.gameStatsDataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

interface GameStatsStore {
    val stats: Flow<GameStats>

    suspend fun saveSettings(settings: GameSettings)
    suspend fun recordCompletedRound(round: RoundHistoryEntry)
    suspend fun clearHistory()
}

class DataStoreGameStatsStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : GameStatsStore {
    constructor(context: Context) : this(context.applicationContext.gameStatsDataStore)

    override val stats: Flow<GameStats> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(::readStats)

    override suspend fun saveSettings(settings: GameSettings) {
        dataStore.edit { preferences ->
            preferences[SELECTED_WORK_IDS] = settings.selectedWorkIds
            preferences[ROUND_LENGTH] = settings.roundLength.coerceIn(MIN_ROUND_LENGTH, MAX_ROUND_LENGTH)
        }
    }

    override suspend fun recordCompletedRound(round: RoundHistoryEntry) {
        dataStore.edit { preferences ->
            val history = decodeHistory(preferences[RECENT_ROUNDS]).toMutableList()
            history.add(0, round)
            preferences[RECENT_ROUNDS] = json.encodeToString(
                ListSerializer(RoundHistoryEntry.serializer()),
                history.take(MAX_RECENT_ROUNDS)
            )
            preferences[COMPLETED_ROUNDS] = (preferences[COMPLETED_ROUNDS] ?: 0) + 1
            preferences[TOTAL_INCORRECT_GUESSES] =
                (preferences[TOTAL_INCORRECT_GUESSES] ?: 0) + round.incorrectGuesses
            preferences[CURRENT_STREAK] = (preferences[CURRENT_STREAK] ?: 0) + 1
            preferences[BEST_STREAK] = maxOf(
                preferences[BEST_STREAK] ?: 0,
                preferences[CURRENT_STREAK] ?: 0
            )
            val best = preferences[BEST_ROUND_SCORE]
            if (best == null || round.incorrectGuesses < best) {
                preferences[BEST_ROUND_SCORE] = round.incorrectGuesses
            }
        }
    }

    override suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences.remove(COMPLETED_ROUNDS)
            preferences.remove(TOTAL_INCORRECT_GUESSES)
            preferences.remove(BEST_ROUND_SCORE)
            preferences.remove(CURRENT_STREAK)
            preferences.remove(BEST_STREAK)
            preferences.remove(RECENT_ROUNDS)
        }
    }

    private fun readStats(preferences: Preferences): GameStats = GameStats(
        settings = GameSettings(
            selectedWorkIds = preferences[SELECTED_WORK_IDS].orEmpty(),
            roundLength = (preferences[ROUND_LENGTH] ?: DEFAULT_ROUND_LENGTH)
                .coerceIn(MIN_ROUND_LENGTH, MAX_ROUND_LENGTH)
        ),
        completedRounds = preferences[COMPLETED_ROUNDS] ?: 0,
        totalIncorrectGuesses = preferences[TOTAL_INCORRECT_GUESSES] ?: 0,
        bestRoundScore = preferences[BEST_ROUND_SCORE],
        currentStreak = preferences[CURRENT_STREAK] ?: 0,
        bestStreak = preferences[BEST_STREAK] ?: 0,
        recentRounds = decodeHistory(preferences[RECENT_ROUNDS])
    )

    private fun decodeHistory(encodedHistory: String?): List<RoundHistoryEntry> = try {
        if (encodedHistory == null) emptyList() else json.decodeFromString(
            ListSerializer(RoundHistoryEntry.serializer()),
            encodedHistory
        )
    } catch (_: Exception) {
        emptyList()
    }

    private companion object {
        const val DEFAULT_ROUND_LENGTH = 10
        const val MIN_ROUND_LENGTH = 1
        const val MAX_ROUND_LENGTH = 100

        val SELECTED_WORK_IDS = stringSetPreferencesKey("selected_work_ids")
        val ROUND_LENGTH = intPreferencesKey("round_length")
        val COMPLETED_ROUNDS = intPreferencesKey("completed_rounds")
        val TOTAL_INCORRECT_GUESSES = intPreferencesKey("total_incorrect_guesses")
        val BEST_ROUND_SCORE = intPreferencesKey("best_round_score")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val RECENT_ROUNDS = stringPreferencesKey("recent_rounds")
    }
}
