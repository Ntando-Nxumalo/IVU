# IVU — Your Multilingual Study Companion

**A warm, intelligent flashcard, quiz, journal & study companion.**

IVU ("It's For You") is a modern, offline-first Android application designed to turn language learning into a sustainable daily habit. By combining the scientifically proven **SM-2 Spaced Repetition (SRS)** algorithm with interactive **Quiz Mode**, **Text-to-Speech (TTS) pronunciation**, reflective journaling, push notifications, and real-time AI assistance, IVU provides a holistic environment for mastering English, isiZulu, and Afrikaans.

Built for the **Open Source Coding (Intermediate)** module (OPSC6312 / POE Part 3) at The Independent Institute of Education.

📦 **Backend Repository:** [ivu-api](https://github.com/Ntando-Nxumalo/ivu-api)  
🌐 **Live API:** Hosted on Render (`https://ivu.onrender.com/`)

---

## ✨ Key Features

### 🧠 Intelligent Learning & Spaced Repetition
- **SM-2 Spaced Repetition Algorithm:** Flashcard review schedules adapt to memory performance (Ease Factor, Interval, Repetitions), ensuring optimal long-term retention.
- **Dynamic Deck Management:** Create, manage, and delete study decks for different target languages (English, isiZulu, Afrikaans) with real-time card counts.
- **Offline-First Architecture:** Flashcards and decks write locally to Room DB instantly (< 5ms) and synchronize asynchronously in the background via WorkManager.

### 🎯 Quiz Mode
- **Flexible Question Types:**
  - **Multiple Choice:** Generates 1 correct answer plus 3 distinct distractors selected from other cards in the deck. Automatically disables if a deck has fewer than 4 cards to prevent duplicate choices.
  - **Type Answer:** Tests exact recall with intelligent normalization (`QuizAnswerChecker`).
  - **Mixed Mode:** Alternates between Multiple Choice and Type Answer questions.
- **Direction Toggle:** Practice Front-to-Back or Back-to-Front (reverse prompt and answer).
- **Smart Answer Validation:** Normalizes input by removing accents/diacritics, stripping surrounding punctuation, collapsing repeated spaces, and ignoring case sensitivity.
- **Missed Question Review:** Summary screen displays score percentage, XP earned, and allows retrying missed questions in a focused session.
- **Responsive Layouts:** Uses `FlowRow` layouts to ensure text never overlaps or clips across long translated strings in English, isiZulu, or Afrikaans.

### 🔊 Text-to-Speech (TTS) Pronunciation
- **Native South African Voices:** Integrates Android's built-in `TextToSpeech` engine mapping language codes to `en-ZA` (with `en-US` fallback), `zu-ZA` (isiZulu), and `af-ZA` (Afrikaans).
- **Pervasive Audio Controls:** Speaker icon buttons on flashcard review cards, card list rows, quiz question prompts, and missed quiz results.
- **Auto-Play Pronunciation:** Optional setting in Preferences automatically speaks card prompt text as each new card appears during review.
- **Speed Adjustment:** Configurable speech rate (Slow `0.75x` / Normal `1.0x`).
- **System Settings Fallback:** Non-crashing fallback launcher opens system TTS settings if a language voice pack is missing on the device.

### 🔔 Notifications & Reminders
- **Firebase Cloud Messaging (FCM):** Receive remote study reminder notifications and test push messages from the server.
- **Daily Local Reminders:** Android `AlarmManager` (`StudyReminderScheduler`) triggers daily local study reminders at your preferred reminder hour (0–23), even when offline.
- **Deep-Linking Badge Alerts:** Celebratory notifications for unlocked badges open directly to `AchievementsActivity` with an animated unlock popup.

### 📓 Reflective Journaling
- **Interactive Calendar:** Full-screen calendar view to track learning history and journal entries.
- **Mood Tracking:** Tag study sessions with moods (Great, Okay, Tough) to correlate emotional states with learning consistency.
- **Contextual Deck Linking:** Link reflection entries to specific study decks.

### 🤖 IVU AI Assist (Powered by Groq)
- **High-Speed AI Study Buddy:** Conversational helper using the `openai/gpt-oss-120b` model via Groq API for near-zero latency responses.
- **Contextual Prompts:** Ask IVU AI to quiz you, explain grammar, or give cultural context for South African vocabulary.

### 🔥 Gamification & Achievements
- **Daily Goals & Streaks:** Maintain study streaks with daily activity tracking.
- **Progression & XP:** Earn 5 XP per correct quiz answer, 20 XP perfect quiz bonus, 10 XP per flashcard review, and 5 XP per journal entry.
- **Badge Framework:** Unlock milestone badges including **First Steps**, **Week Warrior**, **Monthly Master**, **Half Century**, **Reflective**, and **Quiz Whiz**.

### 🎨 Premium Accessibility & Theming
- **Target API 36 (Android 16 Ready):** Built against Android 16 (API 36) with predictive back handling (`BackHandler` / `OnBackPressedDispatcher`).
- **Multilingual Localization:** Complete localized UI strings in **English**, **isiZulu** (`values-zu/`), and **Afrikaans** (`values-af/`).
- **Dynamic Theming:** Seamless support for **Dark Mode** and **Warm Terracotta Light Mode**.

---

## 🧱 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin (100%) |
| **UI Framework** | Jetpack Compose (Material Design 3) |
| **Target SDK** | Android 16 (API Level 36) |
| **Architecture** | MVVM (Model-View-ViewModel) + Offline-First Room |
| **Local Database** | Room Database (SQLite) |
| **Background Sync** | Android WorkManager (`SyncWorker`) |
| **Scheduled Alarms** | Android `AlarmManager` |
| **Audio Engine** | Android `TextToSpeech` (`SpeechManager`) |
| **Notifications** | Firebase Cloud Messaging (FCM) |
| **Authentication** | Firebase Auth (Email/Password + Google SSO) |
| **Networking** | Retrofit 2 + OkHttp 4 |
| **Backend API** | Node.js + Express (Hosted on Render) |
| **AI Engine** | Groq API (GPT-OSS-120B Model) |
| **Code Shrinking** | R8 Minification + Resource Shrinking |

---

## 📂 Project Structure

```text
app/src/main/java/com/ntando/ivu/
├── data/
│   ├── dao/          # Room DAOs (Deck, Flashcard, Journal, UserStats, Pending)
│   ├── database/     # AppDatabase configuration & TypeConverters
│   ├── entity/       # Room Entities (Deck, Flashcard, JournalEntry, UserStats, Badge)
│   ├── model/        # Quiz Models, QuizGenerator, QuizAnswerChecker
│   ├── prefs/        # DataStore Preferences (Theme, Language, Reminders, TTS)
│   └── repository/   # Offline-first repositories bridging Room & REST API
├── network/          # ApiClient, Retrofit Services, AuthInterceptor, FcmTokenManager
├── service/          # IvuMessagingService for FCM push notifications
├── sync/             # SyncManager, SyncWorker for background WorkManager sync
├── ui/
│   ├── achievements/ # Achievements & Badge progress screen
│   ├── auth/         # Login & Registration Compose screens
│   ├── chat/         # IVU AI Assistant chat screen
│   ├── components/   # BottomNav, OfflineBanner, PronunciationButton
│   ├── decks/        # Decks overview & Flashcard list screens
│   ├── journal/      # Calendar & New Journal entry screens
│   ├── quiz/         # Quiz Setup, Question, and Results screens
│   ├── review/       # Flashcard SRS Review screen
│   ├── settings/     # Preference controls, TTS settings, and language selectors
│   └── theme/        # Material 3 Color schemes, Typography, and Locale providers
├── util/             # NotificationHelper, SpeechManager, StudyReminderScheduler
└── viewmodel/        # ViewModels for all features and ViewModelFactory
```

---

## 🚀 How to Install and Use

### Prerequisites
- **Android Device or Emulator:** Running Android 7.0 (API Level 24) or higher.
- **Android Studio:** Android Studio Ladybug / MECL or newer.
- **JDK:** Java 17 toolchain.

### Installation Steps

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/Ntando-Nxumalo/IVU.git
   cd IVU
   ```

2. **Add Firebase Configuration:**
   - Download `google-services.json` from your Firebase project.
   - Place `google-services.json` inside the `app/` root directory.

3. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Build Release APK (with R8 Minification):**
   ```bash
   ./gradlew assembleRelease
   ```

5. **Run Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 📱 How to Use IVU

1. **Authentication:**
   - Open IVU, register an account with email/password or tap **"Continue with Google"**.
2. **Managing Decks & Flashcards:**
   - On the **Decks** screen, tap **"Add Deck"** to create a deck (e.g. "isiZulu Basics").
   - Tap **"Add Card"** on a deck to add vocabulary front/back pairs.
3. **Flashcard Review (SRS):**
   - Tap a deck card to start a Spaced Repetition review session.
   - Tap the card to flip between front and back.
   - Tap the **Speaker icon** to hear pronunciation.
   - Rate card difficulty (**Again**, **Hard**, **Good**, **Easy**) to schedule the next review.
4. **Quiz Mode:**
   - Tap **"Quiz"** on any deck card or from the Flashcard List top bar.
   - Choose your **Mode** (Multiple Choice, Type Answer, Mixed), **Direction**, and **Question Count**.
   - Tap **"Start Quiz"**, answer questions, and view your final score, XP earned, and missed question review.
5. **Reflective Journaling:**
   - Navigate to **Today's Journal** to record daily study reflections, log your mood, and link entries to study decks.
6. **IVU AI Assist:**
   - Tap **"Ask IVU AI"** to ask grammar questions or request vocabulary quizzes from the AI assistant.
7. **Settings & Preferences:**
   - Customize **App Language** (English, isiZulu, Afrikaans), toggle **Dark Theme**, configure **Daily Reminders**, enable **Auto-play Pronunciation**, or adjust **Speech Rate**.

---

## 📋 Project Status (POE Part 3 Completed)

- [x] **Part 1:** Research, Architecture Planning & Design
- [x] **Part 2:** Prototype Development & Core Module Integration
- [x] **Part 3:** Final POE Submission — Quiz Mode, TTS Pronunciation, FCM Push Notifications, Offline WorkManager Sync, Target API 36, Multilingual Localization, and Release Optimization

---

## 🤖 AI Usage Disclosure

During the development of IVU, AI assistance (Google Gemini and Claude via Android Studio’s integrated assistant) was utilized for architectural scaffolding, debugging, and offline-first refactoring.

- **Architectural Scaffolding:** AI generated initial layout boilerplate for Jetpack Compose screens, MVVM ViewModels, and Room DAOs.
- **Offline Sync & Performance:** AI assisted in structuring `SyncWorker` and WorkManager policies to decouple synchronous network calls from local Room writes.
- **TTS & Predictive Back:** AI assisted in configuring locale mapping logic for South African voices and applying Android 16 predictive back handlers (`BackHandler`).

All generated code was manually reviewed, adapted to project requirements, and verified via JUnit unit tests and device Logcat logs.

---

## 📹 Video Walkthrough

🎥 **YouTube Demonstration Video:** [https://youtu.be/GoDcTTftLCU](https://youtu.be/GoDcTTftLCU))

---

## 👤 Authors

**Ntando Nxumalo** — Student Number: `ST10456704`  
**Ayanda Maseko** — Student Number: `ST10443093`  
**Njabulo Fushane** — Student Number: `ST10450253`  

---

## 📄 License
Created as part of the **Open Source Coding (Intermediate)** module (OPSC6312) at The Independent Institute of Education.
