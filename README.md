# IVU — Your Multilingual Study Companion

**A warm, intelligent flashcard, journal & study companion.**

IVU ("It's For You") is a modern Android application designed to turn language learning into a sustainable daily habit. By combining the scientifically proven **SM-2 Spaced Repetition (SRS)** algorithm with reflective journaling and real-time AI assistance, IVU provides a holistic environment for mastering English, isiZulu, and Afrikaans.

Built for the **Open Source Coding (Intermediate)** module (OPSC6312) at The Independent Institute of Education.

📦 **Backend Repository:** [ivu-api](https://github.com/Ntando-Nxumalo/ivu-api)
      or just switch from master to main 
      
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

During the development of IVU, AI tools (specifically Google Gemini and Claude via Android Studio’s integrated AI assistance) were strategically employed to enhance productivity and resolve complex technical hurdles. The use of these tools was categorized into three primary areas: architectural scaffolding, debugging, and cross-platform bridge logic.

**Architectural Scaffolding:** AI was used to generate initial boilerplate code for Jetpack Compose screens and MVVM components (ViewModels and Repositories). This allowed for rapid prototyping of the "terracotta" design system, ensuring consistent padding, typography, and state management across all study modules. By providing high-level UI descriptions, the AI generated the foundational Kotlin code which was then manually refined to implement the SM-2 Spaced Repetition logic and custom gamification triggers.

**Debugging & Troubleshooting:** AI proved invaluable for interpreting complex Gradle build errors and Proguard/R8 obfuscation issues encountered during the build-release cycle. It assisted in identifying a recurring route-registration conflict during the transition to a unified Firebase UID system, saving significant research time. Furthermore, AI tools were used to simulate edge-case scenarios, helping to harden the application’s error handling for network timeouts and database synchronization failures.

**Backend & AI Integration:** The most critical application of AI was the integration of the conversational study assistant. Initially developed using the Gemini API, the backend was later migrated to Groq’s high-performance API to achieve near-zero latency. AI assistance was used to draft the secure proxy logic in Node.js, ensuring that sensitive API keys remained server-side while providing the Android client with an authenticated `/ai/ask` endpoint. 

Every snippet generated by AI was rigorously reviewed, manually adjusted for project-specific constraints, and verified through unit testing (JUnit/MockK) and manual device logs (Logcat). This collaborative approach ensured that while AI accelerated the development pace, the final product strictly adheres to the unique educational objectives and technical standards of the OPSC6312 module.

---

**YOUTUBE URL VIDEO**
https://youtu.be/hkaKmRr35_4
---
## 👤 Author

**Ntando Nxumalo**  
Student Number: St10456704
--
**Ayanda Maseko**
Student Number:St10443093 
--
**Njabulo Fushane**
Student Number: St10450253

---

## 📄 License
This project was created for academic purposes as part of The Independent Institute of Education's OPSC6312 module.
