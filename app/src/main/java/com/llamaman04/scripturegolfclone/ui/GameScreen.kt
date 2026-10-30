package com.llamaman04.scripturegolfclone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.llamaman04.scripturegolfclone.R
import com.llamaman04.scripturegolfclone.domain.GameUiState
import com.llamaman04.scripturegolfclone.domain.ScriptureBook

@Composable
fun GameScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    when (val state = viewModel.uiState.collectAsState().value) {
        GameUiState.Loading -> Text("Loading…", modifier = modifier.padding(24.dp))
        GameUiState.Landing -> LandingScreen(
            onStart = viewModel::openHome,
            modifier = modifier
        )
        is GameUiState.Home -> HomeScreen(
            state = state,
            onToggleWork = viewModel::toggleWork,
            onSelectAllWorks = viewModel::toggleAllWorks,
            onRoundLengthSelected = viewModel::setRoundLength,
            onStartRound = viewModel::startConfiguredRound,
            onViewHistory = viewModel::openHistory,
            modifier = modifier
        )
        is GameUiState.History -> HistoryScreen(
            stats = state.stats,
            onBack = viewModel::closeHistory,
            onClearHistory = viewModel::clearHistory,
            modifier = modifier
        )
        is GameUiState.GuessingBook -> BookGuessScreen(
            state = state,
            books = viewModel.books,
            onBookSelected = viewModel::selectBook,
            onSubmit = viewModel::submitBookGuess,
            onExitRound = viewModel::exitRound,
            modifier = modifier
        )
        is GameUiState.GuessingChapter -> ChapterGuessScreen(
            state = state,
            onSubmit = viewModel::submitChapterGuess,
            onExitRound = viewModel::exitRound,
            modifier = modifier
        )
        is GameUiState.RoundComplete -> RoundCompleteScreen(
            state = state,
            onNextRound = viewModel::nextRound,
            onHome = viewModel::exitRound,
            modifier = modifier
        )
        is GameUiState.Error -> ErrorScreen(
            message = state.message,
            onRetry = viewModel::retryLoading,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookGuessScreen(
    state: GameUiState.GuessingBook,
    books: List<ScriptureBook>,
    onBookSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onExitRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedBook = books.firstOrNull { it.id == state.selectedBookId }?.displayName.orEmpty()

    GuessLayout(
        state.verse.text,
        state.incorrectGuesses,
        state.roundNumber,
        onExitRound,
        modifier,
        confirmExit = true
    ) {
        Text("Guess the book", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedBook,
                onValueChange = {},
                readOnly = true,
                label = { Text("Book") },
                placeholder = { Text("Select a book") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .semantics { contentDescription = "Book selector" }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                books.forEach { book ->
                    DropdownMenuItem(
                        text = { Text(book.displayName) },
                        onClick = {
                            onBookSelected(book.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        Button(onClick = onSubmit, enabled = state.selectedBookId != null, modifier = Modifier.fillMaxWidth()) {
            Text("Submit book")
        }
        Feedback(state.feedback)
        GuessHistory(
            label = "Previous book guesses",
            guesses = state.incorrectBookGuesses.map { id ->
                books.firstOrNull { it.id == id }?.displayName ?: id
            }
        )
    }
}

@Composable
private fun ChapterGuessScreen(
    state: GameUiState.GuessingChapter,
    onSubmit: (Int?) -> Unit,
    onExitRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chapterText by remember(state.verse) { mutableStateOf("") }
    val chapter = chapterText.toIntOrNull()
    val keyboardController = LocalSoftwareKeyboardController.current

    GuessLayout(
        state.verse.text,
        state.incorrectGuesses,
        state.roundNumber,
        onExitRound,
        modifier,
        confirmExit = true
    ) {
        Text("Guess the chapter", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = chapterText,
            onValueChange = { chapterText = it.filter(Char::isDigit) },
            label = { Text("Chapter") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Chapter input" }
        )
        Button(
            onClick = {
                onSubmit(chapter)
                chapterText = ""
                keyboardController?.hide()
            },
            enabled = chapter != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit chapter")
        }
        Feedback(state.feedback)
        GuessHistory(
            label = "Previous chapter guesses",
            guesses = state.incorrectChapterGuesses.map(Int::toString)
        )
    }
}

@Composable
private fun RoundCompleteScreen(
    state: GameUiState.RoundComplete,
    onNextRound: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    GuessLayout(state.result.verse.text, state.result.incorrectGuesses, state.roundNumber, onHome, modifier) {
        Text("Well done!", style = MaterialTheme.typography.headlineSmall)
        Text("${state.result.verse.bookName} ${state.result.verse.chapter}:${state.result.verse.verse}")
        Text("Verses completed: ${state.roundNumber} of ${state.roundLength}")
        Text("Round score: ${state.roundScore}")
        if (state.isRoundFinished) {
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text("Return home")
            }
        } else {
            Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                Text("Next verse")
            }
        }
    }
}

@Composable
private fun GuessLayout(
    verseText: String,
    incorrectGuesses: Int,
    roundNumber: Int,
    onExitRound: (() -> Unit)?,
    modifier: Modifier = Modifier,
    confirmExit: Boolean = false,
    content: @Composable () -> Unit
) {
    var showExitConfirmation by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Scripture Golf", style = MaterialTheme.typography.headlineMedium)
            if (onExitRound != null) {
                IconButton(
                    onClick = {
                        if (confirmExit) showExitConfirmation = true else onExitRound()
                    },
                    modifier = Modifier.semantics { contentDescription = "Return to home" }
                ) {
                    Icon(painterResource(R.drawable.ic_home), contentDescription = "Return to home")
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Verse: $roundNumber")
            Text("Score: $incorrectGuesses")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = verseText,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (showExitConfirmation && onExitRound != null) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Exit this round?") },
            text = { Text("Your progress and score for this round will be lost.") },
            confirmButton = {
                TextButton(onClick = onExitRound) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("Keep playing") }
            }
        )
    }
}

@Composable
private fun Feedback(feedback: String?) {
    if (feedback != null) {
        Text(feedback, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun GuessHistory(label: String, guesses: List<String>) {
    if (guesses.isNotEmpty()) {
        Text(
            "$label: ${guesses.joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Unable to load Scripture Golf", style = MaterialTheme.typography.headlineSmall)
        Text(message)
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
