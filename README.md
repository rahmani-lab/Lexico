# LingoDo

A professional, fully-offline **multi-language vocabulary flashcard app for Android** (formerly
Lexico). LingoDo combines rich, language-learning–focused flashcards with a spaced-repetition study
system, built-in text-to-speech pronunciation, global **language-pair workspaces**, and progress
statistics.

> The app interface is in **English**. Card content — the meaning and example translations — can be
> written in **any language**; right-to-left scripts such as Persian and Arabic are rendered
> correctly inside their fields. Package/app id: `com.rahmanilab.lingodo`.

## LingoDo upgrade (multi-lingual)
- **Global language-pair manager** — decks/cards belong to a source→target pair (e.g. Persian →
  German). Switching the active pair filters everything and adapts the TTS voice to the target
  language. Manage pairs from the Home globe button or Settings → Language pairs.
- **Auto-fill (✨, optional & user-triggered)** — tap the spark button next to a word to enrich the
  card. It only fills **empty** fields (never overwrites your text) and is designed to auto-detect
  the grammatical class and extract **word forms & inflections** (verb tenses, comparatives, word
  family). *Base-First build: the engine, BYOK key handling and merge logic ship now; the live
  network fetch turns on in the next update.*
- **Word forms & inflections** — a structured, editable list on every card, shown on the review back.
- **BYOK API keys** — bring your own Gemini/Groq/DeepSeek/OpenAI/Claude key; stored **encrypted** via
  the Android Keystore (AES-256-GCM), used only for direct requests to the provider you pick.
- **Help & onboarding center** — an in-app guide (workflow, language pairs, API keys, import/export,
  TTS troubleshooting).
- **Anki import** — import Anki "Notes in Plain Text" (.txt/TSV) alongside CSV/JSON.

---

## Features

### Flashcards
**Front of the card**
- English word or phrase, part of speech, and phonetic transcription (e.g. `/rɪˈzɪliənt/`)
- 🔊 Tap-to-play pronunciation (and a slow-playback button)
- Optional short pronunciation hint (e.g. `ri-ZIL-ee-uhnt`)

**Back of the card**
- Meaning (in any language) and a simple English definition
- Image (added from the gallery via the Android Photo Picker)
- **Multiple** example sentences, each with its own translation
- Synonyms, antonyms, and common collocations
- Personal note and the source/context where you first saw the word

### Card management
- Create, edit, and delete cards and decks
- Organise with decks and free-form **tags**
- Full-text search across words, meanings, definitions, notes, and sources
- **Duplicate detection** — warns when a word already exists in a deck
- First-class support for **phrases and phrasal verbs**, not just single words
- **Import & export** cards as CSV or JSON

### Study system
Four answers while reviewing — **Again / Hard / Good / Easy** — feed a spaced-repetition
scheduler that picks each card's next due date. You can choose between two schedulers in Settings:
the classic **SM-2** or **FSRS**. Every review is stored from day one (see
[Schedulers](#schedulers--sm-2--fsrs)).

### Data, backup & cloud
- **CSV / JSON import & export** of your cards, for sharing or migrating
- **Full backup & restore** — a complete JSON snapshot including schedules and the entire review
  history
- **Cloud backup** — all file access goes through Android's document picker, so a backup can be
  saved straight to Google Drive, Dropbox, or any storage provider on the device (and restored the
  same way)

### Language-learning extras
- Auto-play pronunciation when a card appears
- Choose an **American** or **British** accent (globally or per card)
- Show/hide the phonetic transcription
- Four review modes: **Word → Meaning**, **Meaning → Word** (reverse), **Type the word**, and
  **Fill-in-the-blank** (cloze from an example sentence)
- Typed answers are checked leniently against the correct word
- Slow pronunciation playback
- Browse **Difficult words** (cards that have lapsed repeatedly)
- Timed sessions (5 / 10 / 20 minutes, or unlimited)
- New-cards-per-day limit
- Daily / weekly stats, accuracy, and a study **streak**
- Daily study **reminder** notification (WorkManager)

---

## Tech stack

| Concern            | Choice                                           |
|--------------------|--------------------------------------------------|
| Language           | Kotlin                                           |
| UI                 | Jetpack Compose + Material 3                      |
| Architecture       | ViewModel + `StateFlow`, unidirectional data flow, Repository pattern |
| Persistence        | Room (SQLite)                                     |
| Preferences        | DataStore (Preferences)                          |
| Background work    | WorkManager                                      |
| Pronunciation      | Android `TextToSpeech`                            |
| Images             | Android Photo Picker + Coil                      |
| Dependency wiring  | Lightweight manual DI (`AppContainer`)           |

Minimum SDK 26 · Target SDK 34 · JDK 17.

---

## Architecture

Lexico follows the official Android app architecture: a **UI layer** (Compose screens + ViewModels)
that depends on a **data layer** (repositories over Room and DataStore), with a small **domain**
package for framework-independent logic (the scheduler and shared models).

```
com.rahmanilab.lingodo
├── LexicoApplication / MainActivity
├── di/                      # AppContainer – manual DI, owns all singletons
├── domain/
│   ├── model/               # Rating, CardState, ReviewMode, Example, WordForm, Language, AiProvider…
│   ├── scheduler/           # Scheduler interface + Sm2Scheduler + FsrsScheduler (pure, tested)
│   └── autofill/            # AutoFillEngine interface + result models
├── data/
│   ├── local/               # Room: entities (incl. language_pairs), DAOs, converters, database + migration
│   ├── preferences/         # SettingsRepository (DataStore) + settings models
│   ├── repository/          # Deck / Card / Review / Stats / LanguagePair / AiConfig repositories
│   ├── security/            # SecureKeyStore (Android Keystore AES-GCM for BYOK keys)
│   ├── autofill/            # DefaultAutoFillEngine (3-tier orchestrator)
│   ├── backup/              # ImportExportRepository (CSV / JSON / Anki) + full backup snapshot
│   └── DatabaseSeeder.kt    # First-run default pair + sample deck
├── tts/                     # PronunciationManager (TextToSpeech wrapper)
├── work/                    # ReminderWorker + ReminderScheduler
└── ui/
    ├── theme/ · navigation/ · components/
    ├── home/ · decks/ · editcard/ · review/ · browse/ · statistics/ · settings/
    ├── workspace/           # Language-pair manager
    ├── help/                # Help & onboarding center
    └── LexicoApp.kt         # NavHost + bottom navigation
```

The Room database (`lingodo.db`, schema v2) adds a `language_pairs` table plus `languagePairId` on
decks and `wordForms` on cards, with a non-destructive `MIGRATION_1_2`.

ViewModels are created from the `AppContainer` via `viewModelFactory { initializer { … } }`, so
navigation arguments arrive through `SavedStateHandle` and no annotation-processing DI framework is
required.

### Data model
- `DeckEntity` — a collection of cards
- `CardEntity` — the word and all its content (structured lists such as examples and synonyms are
  stored as JSON via Room type converters, so a card can hold several of each)
- `CardScheduleEntity` — spaced-repetition state (due date, interval, ease, lapses…). It also keeps
  reserved, nullable `difficulty`/`stability` columns for a future FSRS scheduler
- `ReviewLogEntity` — an immutable record of every answer
- `TagEntity` + `CardTagCrossRef` — many-to-many tags

### Schedulers — SM-2 & FSRS
Two schedulers implement the same small, pure `Scheduler` interface and are selectable in Settings:

- **`Sm2Scheduler`** — a deterministic SM-2 (SuperMemo) variant with Anki-like learning steps and an
  ease factor.
- **`FsrsScheduler`** — an [FSRS](https://github.com/open-spaced-repetition) implementation
  (FSRS-5 default weights) that models memory as **difficulty**, **stability** and
  **retrievability**, producing more accurate intervals. The reserved `difficulty`/`stability`
  columns on the schedule table persist its state.

Both are pure functions (no Android/Room dependencies) and are covered by unit tests. Because
**every review is logged from day one**, switching schedulers — or retraining FSRS on your own
history later — never loses data.

---

## Building & running

Requirements: Android Studio (Koala or newer) with a JDK 17 and the Android SDK (compileSdk 34).

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug

# Run the unit tests
./gradlew testDebugUnitTest
```

Or simply open the project in Android Studio and press **Run**.

> This repository contains the complete source and Gradle wrapper. Dependencies are fetched from
> Google's Maven and Maven Central on the first build.

## Testing
- `Sm2SchedulerTest` — learning steps, graduation, interval growth, lapses, ease floor, previews
- `FsrsSchedulerTest` — memory-state init, monotonic intervals by grade, lapse handling, bounds
- `TextUtilsTest` — answer normalisation and cloze blanking

---

## Roadmap
- Optional recorded / dictionary audio (Cambridge, Oxford, …) alongside TTS
- Account-based automatic cloud sync (the current backup format is already sync-ready)
- Home-screen widgets
- Per-user FSRS parameter optimisation from the stored review history

## License
MIT — see [LICENSE](LICENSE).
