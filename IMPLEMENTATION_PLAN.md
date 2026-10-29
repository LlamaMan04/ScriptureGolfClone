# Scripture Golf Clone — Android Implementation Plan

## 1. Product goal

Build an offline-friendly Android game inspired by **LDS Scripture Golf**. Each round presents the text of a randomly selected verse from LDS scripture. The player first guesses the book; only after the book is correct may the player begin guessing the chapter. The player continues through the book and chapter phases until both are correct; the number of incorrect guesses becomes that round's score. The app then records the result and starts another round when requested.

The first release should prioritize a simple, reliable single-player experience:

- Select a random verse from the approved scripture corpus.
- Display the verse text without revealing its book, chapter, or verse reference.
- Let the player choose or enter the book first, then unlock the chapter guess only after the book is correct.
- Keep the round active after incorrect guesses and show which phase still needs to be completed.
- Use the number of incorrect guesses as the round score, with fewer guesses being better.
- Track the current score and basic game history locally.
- Work without a network connection after installation.

The existing project is already configured as a Kotlin Jetpack Compose application. The implementation should grow from the current `MainActivity` and theme files rather than introducing XML-based screens unless a platform integration requires it.

## 1.1 Current implementation status

The core playable prototype is implemented. The repository currently includes:

- Compose landing, home/configuration, gameplay, completion, and catalog-error screens.
- ViewModel-backed work selection, round length selection, multi-verse rounds, exit handling, and catalog retry handling.
- Sequential book-then-chapter guessing. Chapter guessing is unavailable until the book is correct, incorrect guesses increase the score, and the same verse remains active after an incorrect guess.
- An offline asset-backed catalog with an index, per-book JSON files, lazy book loading, random verse selection, previous-verse avoidance, canonical book IDs, and catalog validation.
- Accessibility semantics for the main selectors, inputs, controls, and navigation actions.
- JVM tests covering game rules, ViewModel progression, catalog parsing, catalog validation, indexed metadata, and deterministic random selection.

The prototype catalog currently contains one indexed chapter/verse for each of six books across five scripture works. It is sufficient for development, but it is not yet a complete release catalog.

The following planned capabilities are not implemented yet:

- DataStore-backed persistence for completed rounds, scores, streaks, and recent history.
- A statistics/history screen and persistent user preferences.
- Compose UI tests and an instrumentation smoke test.
- A complete, licensing-verified scripture catalog and production content review.
- Final offline-device, rotation/process-recreation, release-build, and accessibility verification.

The latest local test attempt could not start because the Gradle wrapper cache lock at `C:\Users\Stormblessed\.gradle\wrapper\dists\gradle-9.6.0-bin\...zip.lck` was inaccessible. Test status should be confirmed after that local Gradle environment issue is cleared.

## 2. Recommended architecture

Use a small MVVM-style architecture with a repository boundary:

```text
Compose UI
    |
    v
GameViewModel  ---- observes ---->  GameUiState
    |
    v
ScriptureRepository
    |                         \
    v                          v
Local scripture catalog        Local game-stat storage
JSON/raw resource              DataStore Preferences
```

### Presentation layer

- `MainActivity`: creates the Compose content and applies `ScriptureGolfCloneTheme`.
- `GameScreen`: renders the active game state.
- `GameViewModel`: owns game control flow, validates guesses, calculates scores, and exposes a `StateFlow<GameUiState>`.
- Reusable composables for the verse card, guess controls, score header, result panel, and settings/help actions.

The UI should render from state rather than maintaining separate mutable state for the game. This makes rotation, process recreation, previews, and automated tests easier.

### Domain layer

Keep game rules independent of Android UI:

- `ScriptureVerse`: immutable verse data.
- `ScriptureBook`: canonical book metadata and chapter count.
- `Guess`: the player's current guess for the active phase; chapter guesses are not valid until the book phase is complete.
- `GuessEvaluation`: the active phase, whether that phase's guess is correct, and the number of incorrect guesses added by the submission.
- `RoundResult`: the final reference, total incorrect guesses, and completion status.
- `GameRules`: pure functions for evaluating each part and calculating the round score.
- `RandomVerseSelector`: chooses a verse using an injectable random source so tests can be deterministic.

### Data layer

- `ScriptureRepository`: provides books, chapters, and random verses to the ViewModel.
- `ScriptureCatalogDataSource`: reads the bundled scripture catalog from `res/raw/scripture_catalog.json` or a generated Kotlin resource.
- `GameStatsStore`: persists score, rounds played, streak, and recent results with Jetpack DataStore Preferences.

Avoid coupling the ViewModel directly to JSON parsing or Android `Context` access.

Implementation note: the repository and catalog data source are present and use `assets/scriptures/index.json` plus per-book JSON files. `GameStatsStore` and DataStore have not been added yet; the current score and round state live only in the ViewModel for the active process.

## 3. Screen and component layout

### Main game screen

Use a vertically scrolling `Scaffold` with a compact top app bar and content centered in the available width.

```text
Scaffold
├── TopAppBar
│   ├── title: Scripture Golf
│   ├── score/streak summary
│   └── overflow: How to Play / Reset Game
└── GameScreen content
    ├── Round indicator and score
    ├── VerseCard
    │   ├── “What is the reference?” prompt
    │   └── verse text
    ├── GuessForm
    │   ├── book guess selector and Submit Book Guess button
    │   ├── chapter guess input/dropdown (disabled until book is correct)
    │   └── Submit Chapter Guess button
    ├── GuessFeedback (after each submission)
    │   ├── current phase: guess book / guess chapter
    │   ├── book: correct / try again
    │   ├── chapter: locked until book is correct, then correct / try again
    │   └── incorrect guess count for this round
    ├── RoundCompleteCard (only after both are correct)
    │   ├── correct reference
    │   ├── final round score
    │   └── Next Verse button
    └── optional progress/stats summary
```

### Interaction details

- The verse card remains visible while the player chooses an answer.
- The book selector is enabled at the start of a round; the chapter input and chapter submit button are disabled.
- The player must submit a book guess before any chapter guess is accepted. An incorrect book guess leaves the round in the book phase and increments the incorrect-guess count.
- When the book is correct, lock the book selector, enable the chapter input, and move the round to the chapter phase.
- During the chapter phase, an incorrect chapter guess leaves the same verse active and increments the incorrect-guess count.
- Do not increment the count for invalid, unsubmitted input or for the correct book guess that advances the phase.
- Once the chapter is correct, disable the guess controls, show the complete reference, and use the incorrect-guess count as the final score for the round.
- The Next Verse button clears the previous guesses and feedback, then loads a new verse.
- Preserve the active round across configuration changes by keeping it in the ViewModel.
- Use accessible labels and content descriptions for dropdowns, buttons, and feedback states.

### Optional screens for a later iteration

- **Statistics screen:** total rounds, accuracy, average score, best streak, and recent guesses.
- **Settings screen:** translation/corpus selection, difficulty, sound/vibration, and reset data.
- **How to Play dialog:** explain scoring and the available scripture books.

## 4. Data model and storage

### Scripture catalog

Bundle the initial catalog with the app so the game works offline. A JSON structure is easy to author and inspect:

```json
{
  "version": 1,
  "translation": "King James Version",
  "books": [
    {
      "id": "1-nephi",
      "displayName": "1 Nephi",
      "testament": "Book of Mormon",
      "chapters": [
        {
          "number": 1,
          "verses": [
            { "number": 1, "text": "..." }
          ]
        }
      ]
    }
  ]
}
```

The production catalog should contain only scripture text and metadata that the project has permission to distribute. Start with a small verified sample for development, then expand the catalog before release. Validate the catalog on app startup or in a build-time test so malformed references cannot create unusable rounds.

### Runtime data classes

```kotlin
data class ScriptureVerse(
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class Guess(val bookId: String, val chapter: Int)

data class GuessEvaluation(
    val bookCorrect: Boolean,
    val chapterCorrect: Boolean,
    val incorrectGuessesAdded: Int
)

data class RoundResult(
    val verse: ScriptureVerse,
    val guess: Guess,
    val incorrectGuesses: Int,
    val isComplete: Boolean
)
```

### Persistence choices

Use **DataStore Preferences** for small player data:

- total rounds played
- completed rounds
- total incorrect guesses
- best (lowest) round score
- optional current/best streak of completed rounds
- last selected book, if desired
- optional serialized recent results with a small size limit

Do not store the full scripture catalog in DataStore. It is static app content and belongs in bundled resources. If history, favorites, multiple translations, or user-created collections become substantial, migrate game history to Room with entities for rounds and saved verses.

## 5. Program control and round lifecycle

Represent the game with an explicit state machine. Suggested states:

```text
Loading
  -> GuessingBook
  -> GuessFeedback (after a book submission)
  -> GuessingBook (if book is wrong)
  -> GuessingChapter (when book is correct)
  -> GuessFeedback (after a chapter submission)
  -> GuessingChapter (if chapter is wrong)
  -> RoundComplete (when chapter is correct)
  -> GuessingBook (next round)

Any state -> Error(message) when catalog or persistence loading fails
```

Suggested `GameUiState`:

```kotlin
sealed interface GameUiState {
    data object Loading : GameUiState
    data class GuessingBook(
        val verse: ScriptureVerse,
        val selectedBookId: String? = null,
        val incorrectGuesses: Int = 0,
        val completedRounds: Int = 0,
        val roundNumber: Int = 1
    ) : GameUiState
    data class GuessingChapter(
        val verse: ScriptureVerse,
        val selectedChapter: Int? = null,
        val incorrectGuesses: Int,
        val completedRounds: Int,
        val roundNumber: Int
    ) : GameUiState
    data class GuessFeedback(
        val verse: ScriptureVerse,
        val guess: Guess,
        val evaluation: GuessEvaluation,
        val incorrectGuesses: Int,
        val roundNumber: Int
    ) : GameUiState
    data class RoundComplete(
        val verse: ScriptureVerse,
        val result: RoundResult,
        val bestRoundScore: Int?,
        val roundNumber: Int
    ) : GameUiState
    data class Error(val message: String) : GameUiState
}
```

### Startup

1. `MainActivity` creates the ViewModel through the normal lifecycle-aware Compose integration.
2. The ViewModel loads the catalog and saved stats concurrently when possible.
3. Once data is available, it selects a random verse and emits `GuessingBook`; the chapter control is disabled.
4. If the catalog is unavailable, emit a recoverable error with a retry action.

### Guess submission and retries

1. Read only the guess for the current phase: book first, then chapter.
2. Reject incomplete or invalid input without changing the round or score.
3. During `GuessingBook`, compare the submitted book to the verse's canonical book ID.
4. If the book is wrong, add one to the incorrect-guess count and remain in the book phase. If it is correct, lock the book and transition to `GuessingChapter` without adding to the count.
5. During `GuessingChapter`, compare the submitted chapter to the verse's chapter.
6. If the chapter is wrong, add one to the incorrect-guess count and remain in the chapter phase. If it is correct, create `RoundResult`, persist the completed round, and emit `RoundComplete`.
7. Emit `GuessFeedback` after every valid submission, identifying the active phase and whether its guess was correct.

The ViewModel must not generate a new verse after an incorrect guess. The same verse remains active until the player has correctly identified both its book and chapter.

### Next verse

1. Select a new verse, preferably avoiding the immediately previous verse.
2. Reset the book and chapter guesses, feedback, and incorrect-guess count; start in the book phase with the chapter control disabled.
3. Increment the round number.
4. Emit `GuessingBook` with the chapter control disabled.

Use `viewModelScope` for repository and DataStore work. Keep button event handlers idempotent where practical so repeated taps cannot award points twice.

## 6. Scoring and difficulty

Make scoring configurable in `GameRules` instead of embedding constants in composables. The first version should use a golf-like low-score model:

- The round score is the total number of incorrect book guesses plus incorrect chapter guesses made before completion.
- A correct guess contributes zero to the score.
- Chapter guesses cannot be submitted while the book is incorrect or has not yet been guessed.
- The round ends only after the book is correct and the chapter is correct.
- Lower scores are better; a perfect round has a score of zero.
- Persist the completed round only after both parts are correct.

Possible future difficulty modes:

- **Easy:** show a list of books first, then a list of chapters after the book is correct.
- **Standard:** choose a book from a dropdown first, then type the chapter.
- **Hard:** type both book and chapter, with fewer hints.

The UI can support these modes later without changing the underlying `Guess` or repository contracts.

## 7. Dependency and project additions

Add only the libraries needed for the first release:

- Lifecycle ViewModel Compose integration.
- Kotlin coroutines and Flow support if not already available transitively.
- Jetpack DataStore Preferences.
- Optional Material 3 components already present in the project.

Suggested package structure:

```text
com.llamaman04.scripturegolfclone
├── data
│   ├── ScriptureRepository.kt
│   ├── ScriptureCatalogDataSource.kt
│   └── GameStatsStore.kt
├── domain
│   ├── ScriptureModels.kt
│   ├── GameRules.kt
│   └── RandomVerseSelector.kt
├── ui
│   ├── GameScreen.kt
│   ├── GameViewModel.kt
│   ├── components/
│   └── theme/
└── MainActivity.kt
```

## 8. Testing strategy

- Unit-test `GameRules` for sequential book-then-chapter correctness, incorrect-guess counting, completion, and invalid chapters.
- Unit-test random selection with a seeded or fake random source.
- Test JSON/catalog validation, including an empty catalog and malformed references.
- Test the ViewModel's state transitions: startup in book phase, incorrect book retry, transition to chapter phase, incorrect chapter retry, completion, and next round.
- Add Compose UI tests confirming the chapter control is disabled before the book is correct, then enabled afterward, along with feedback, completion, and next-round behavior.
- Add an instrumentation test that launches the app and confirms a verse and guess controls are visible.

Current coverage includes JVM tests for the domain rules, main ViewModel book/chapter progression, catalog parsing and validation, indexed metadata, and deterministic random selection. Compose UI and instrumentation coverage remains to be added.

## 9. Implementation milestones

1. **Complete:** Replace the starter greeting with Compose landing, home/configuration, gameplay, completion, and error screens.
2. **Complete:** Add domain models, game rules, ViewModel state transitions, multi-verse round length, and work selection.
3. **Complete:** Add the bundled asset catalog, repository abstraction, lazy book loading, random selection, and catalog validation.
4. **Complete:** Add book/chapter selection, sequential phase locking, incorrect-guess feedback, score display, exit confirmation, and next-round behavior.
5. **Partially complete:** Add automated coverage for domain rules, ViewModel progression, parsing, validation, and repository selection. Compose UI tests and the instrumentation smoke test remain.
6. **Remaining:** Add DataStore-backed completed-round statistics, best score/streak/history, and a statistics/settings surface if required for the first release.
7. **Remaining:** Expand the catalog beyond the development sample, verify distribution rights/content accuracy, and validate catalog size.
8. **Remaining:** Resolve the local Gradle wrapper-cache lock, run unit/UI/instrumentation tests, and perform release-build, offline, configuration-change/process-recreation, and accessibility checks.

## 10. Important risks and decisions

- **Scripture licensing/content:** confirm that the selected text and translation may be distributed with the app.
- **Catalog size:** a complete corpus may require compressed resources or a more efficient generated format; measure APK size before release.
- **Randomness and repetition:** inject the random source and track the previous reference so behavior is testable and consecutive duplicates can be avoided.
- **Answer matching:** compare canonical book IDs, not display strings, so punctuation and localized labels do not affect correctness.
- **Offline behavior:** treat the bundled catalog as the source of truth and make network synchronization an optional future feature, not a requirement for gameplay.
