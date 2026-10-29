package com.llamaman04.scripturegolfclone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.llamaman04.scripturegolfclone.domain.GameUiState

@Composable
fun HomeScreen(
    state: GameUiState.Home,
    onToggleWork: (String) -> Unit,
    onSelectAllWorks: () -> Unit,
    onRoundLengthSelected: (Int) -> Unit,
    onStartRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSelected = state.selectedWorkIds.size == state.works.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Scripture Golf", style = MaterialTheme.typography.headlineMedium)
        Text("Build a round and identify each scripture reference with as few incorrect guesses as possible.")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Scripture works", style = MaterialTheme.typography.titleLarge)
                Text("Choose which works may provide verses. Books are included automatically within each work.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { onSelectAllWorks() },
                        modifier = Modifier.semantics { contentDescription = "Select all scripture works" }
                    )
                    Text("All works")
                }
                state.works.forEach { work ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = work.id in state.selectedWorkIds,
                            onCheckedChange = { onToggleWork(work.id) },
                            modifier = Modifier.semantics {
                                contentDescription = "Include ${work.displayName}"
                            }
                        )
                        Column {
                            Text(work.displayName)
                            Text(
                                "${work.bookCount} ${if (work.bookCount == 1) "book" else "books"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    "${state.selectedWorkIds.size} of ${state.works.size} works selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Round length", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10, 20).forEach { length ->
                        if (state.roundLength == length) {
                            Button(onClick = { onRoundLengthSelected(length) }) { Text("$length") }
                        } else {
                            OutlinedButton(onClick = { onRoundLengthSelected(length) }) { Text("$length") }
                        }
                    }
                }
                Text("${state.roundLength} verses in this round")
            }
        }

        Button(
            onClick = onStartRound,
            enabled = state.selectedWorkIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Start scripture golf round" }
        ) {
            Text("Start round")
        }
    }
}
