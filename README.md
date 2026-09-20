# IVU — Your Multilingual Study Companion

**A warm, intelligent flashcard, journal & study companion.**

IVU ("It's For You") is a modern Android application designed to turn language learning into a sustainable daily habit. By combining the scientifically proven **SM-2 Spaced Repetition (SRS)** algorithm with reflective journaling and real-time AI assistance, IVU provides a holistic environment for mastering English, isiZulu, and Afrikaans.

Built for the **Open Source Coding (Intermediate)** module (OPSC6312) at The Independent Institute of Education.

📦 **Backend Repository:** [ivu-api](https://github.com/Ntando-Nxumalo/ivu-api)
🌐 **Live API:** Hosted on Render

---

## ✨ Features

### 🧠 Intelligent Learning
- **SM-2 Spaced Repetition:** Flashcard reviews are scheduled based on your memory performance, ensuring long-term retention.
- **Dynamic Decks:** Create, manage, and delete study decks for different languages with real-time card counting and progress tracking.

### 📓 Reflective Journaling
- **Paginated Calendar:** A full-screen interactive calendar with **swipe-to-navigate** month views to track your study history.
- **Mood Tracking:** Tag study sessions with moods (Great, Okay, Tough) to correlate emotional state with learning progress.
- **Contextual Linking:** Link specific journal entries to the deck you studied that day.

### 🤖 IVU AI Assist (Powered by Groq)
- **Instant Assistance:** A high-speed conversational study helper using the `openai/gpt-oss-120b` model via Groq for near-zero latency.
- **Smart Quizzing:** Ask IVU to quiz you on your cards, explain complex grammar, or provide cultural context for new words.

### 🔥 Gamification & Progress
- **Daily Goals:** A 10-card daily review target designed for fast leveling and consistent engagement.
- **Progression System:** Earn XP, level up (e.g., "Level 5 · Wordsmith"), and maintain daily study streaks.
- **Badge Framework:** Unlock 5 unique milestone badges (First Review, 7-Day Streak, Monthly Master, etc.) with real-time unlock notifications.

### 🎨 Premium UI/UX
- **Warm Terracotta Theme:** A custom color palette designed to reduce "study anxiety" and provide a comfortable learning environment.
- **Full Localization:** Complete UI synchronization in **English, isiZulu, and Afrikaans**.
- **Dynamic Theming:** Seamless support for **Dark Mode** and **Warm Light Mode** across all screens.

---

## 🧱 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Local Persistence** | Room Database (SQLite) |
| **Authentication** | Firebase Auth (Email/Pass + Google SSO) |
| **Remote Database** | Google Cloud Firestore |
| **Networking** | Retrofit 2 + OkHttp 4 |
| **Backend** | Node.js + Express (Hosted on Render) |
| **AI Engine** | Groq API (GPT-OSS-120B Model) |
| **CI/CD** | GitHub Actions (Automated Unit Testing & Builds) |

---

## 📂 Project Structure

```text
app/src/main/java/com/ntando/ivu/
├── data/
│   ├── database/       # Room database & TypeConverters
│   ├── dao/            # Data Access Objects for local sync
│   ├── entity/         # UserStats, Badges, and User entities
│   ├── prefs/          # DataStore for Theme & Language persistence
│   └── repository/     # Repositories bridging Firestore + Local Room
├── ui/
│   ├── components/     # Shared UI (Synced Bottom Navigation)
│   ├── theme/          # Material 3 Color schemes & Typography
│   ├── auth/           # Bolder, accessible Login & Register screens
│   ├── journal/        # Paginated Calendar & Entry screens
│   └── achievements/   # Unified scrolling Progress & Badge grid
└── viewmodel/          # State management for all core features
```

---

## 🚀 Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Ntando-Nxumalo/IVU.git
   ```
2. **Setup Firebase:** Add your `google-services.json` to the `app/` directory.
3. **Environment:** Ensure the `ivu-api` is running and the `BASE_URL` in `ApiClient.kt` is updated.
4. **Build:** Run `./gradlew assembleDebug` or open in Android Studio.

---

## 📋 Project Status

- [x] **Part 1:** Research, Planning & Design
- [x] **Part 2:** Prototype Development (Current)
- [ ] **Final PoE:** Play Store Prep & Offline Sync Optimization

---

## 🤖 AI Usage Disclosure

During the development of this project, AI assistance (Gemini/Claude) was utilized for a specific, high-level task:

- **AI Integration Logic:** The AI provided the technical scaffolding for proxying AI requests through the Node.js backend. This included configuring the initial fetch requests to Gemini and the subsequent migration to the **Groq API** to ensure security (keeping API keys off the client device) and high performance.

All other core application logic, including the **SM-2 Spaced Repetition implementation**, **Room/Firestore data synchronization**, **paginated calendar architecture**, and the **custom terracotta UI system**, was authored and refined by the developer to meet the specific requirements of the OPSC6312 module.

---

## 👤 Author

**Ntando Nxumalo**  
Student Number: *[Insert Your Number]*

---

## 📄 License
This project was created for academic purposes as part of The Independent Institute of Education's OPSC6312 module.
