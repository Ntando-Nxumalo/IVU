# IVU - Multilingual Study Companion 📖🎓

IVU is a premium study application designed to help users master new languages and subjects through flashcards, active recall, and AI-driven insights. It combines modern UI with gamification to make learning Zulu, Afrikaans, and other languages engaging and simple.

🎯 **Purpose & Audience**
**What the app does:**
IVU simplifies learning by allowing users to create study decks, track progress with spaced repetition, and maintain a learning journal. It provides visual feedback through study streaks and rewards consistency with a leveling system.

**Who it's for:**
The app is designed for students, language learners, and anyone looking for a "gamified" approach to studying. It's particularly useful for those learning South African languages like Zulu and Afrikaans.

📱 **Demonstration**
*(Video link to be updated)*

🎨 **Design Decisions**
- **MVVM Architecture:** Chosen to ensure a clean separation between UI logic and data handling.
- **Hybrid UI Strategy:** Jetpack Compose for complex, state-driven components and traditional XML Layouts for standard Activity structures.
- **Material 3 Design:** Leveraged for a modern, accessible, and "premium" feel.
- **Repository Pattern:** Centralizes data access from the Room database.

🛠 **GitHub & GitHub Actions**
- **Version Control:** GitHub feature-branch workflow.
- **CI/CD Automation:** GitHub Actions for automated builds and testing.

🌟 **Custom Features**
1. 🤖 **IVU AI Assist**
A built-in study assistant that uses natural language to explain concepts, generate example sentences in different languages, and quiz you on your decks.
2. 📊 **Gamified Progress**
Track study streaks and earn badges like "Card Master" and "7-Day Streak" to stay motivated.

🏗 **Technical Details**
- **UI Framework:** Jetpack Compose & XML (Hybrid).
- **Database:** Room Persistence Library.
- **Navigation:** Custom Radial Navigation Menu.
- **Concurrency:** Kotlin Coroutines and Flow.

🔧 **Installation**
1. Clone the repository.
2. Open the project in Android Studio (Ladybug 2024.2.1 or newer).
3. Ensure Android SDK 35 is installed.
4. Sync Gradle and run.

📦 **Key Dependencies**
- `androidx.room`: Local data storage.
- `androidx.compose`: Modern UI toolkit.
- `androidx.lifecycle`: ViewModel and Lifecycle management.
- `kotlinx.coroutines`: Asynchronous programming.
