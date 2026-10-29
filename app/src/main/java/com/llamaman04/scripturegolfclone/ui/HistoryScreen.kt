package com.llamaman04.scripturegolfclone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.llamaman04.scripturegolfclone.domain.GameStats
import com.llamaman04.scripturegolfclone.domain.RoundHistoryEntry
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    stats: GameStats,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Playing history", style = MaterialTheme.typography.headlineMedium)
        Text("Your most recent completed verses and scores.")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Summary", style = MaterialTheme.typography.titleLarge)
                SummaryRow("Completed verses", stats.completedRounds.toString())
                SummaryRow("Total incorrect guesses", stats.totalIncorrectGuesses.toString())
                SummaryRow("Best score", stats.bestRoundScore?.toString() ?: "—")
                SummaryRow("Best streak", stats.bestStreak.toString())
            }
        }

        Text("Recent verses", style = MaterialTheme.typography.titleLarge)
        if (stats.recentRounds.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No completed verses yet. Start a round to build your history.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            stats.recentRounds.forEach { round -> HistoryEntry(round) }
        }

        OutlinedButton(
            onClick = { showClearConfirmation = true },
            enabled = stats.completedRounds > 0 || stats.recentRounds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear history")
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to home")
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear playing history?") },
            text = { Text("This removes all completed verses and score statistics. Your round settings will be kept.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearHistory()
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HistoryEntry(round: RoundHistoryEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "${round.bookName} ${round.chapter}:${round.verse}",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Incorrect guesses: ${round.incorrectGuesses}")
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(round.completedAtEpochMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
