package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.ui.journal.JournalCalendarScreen
import com.ntando.ivu.ui.journal.NewJournalEntryScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.JournalViewModel
import com.ntando.ivu.viewmodel.JournalUiState
import com.ntando.ivu.viewmodel.ViewModelFactory
import java.util.*

/**
 * [JournalActivity] manages the user's reflection and learning journal entries.
 *
 * Intent Extras:
 * - `action` (String?): If set to `"NEW_ENTRY"`, immediately opens the new entry creation dialog upon start.
 *
 * Features:
 * - Displays a interactive journal calendar using [JournalCalendarScreen].
 * - Displays a pop-up dialog ([NewJournalEntryScreen]) for adding entries with mood, text, and optional deck links.
 * - Handles navigation routing across IVU tabs.
 */
class JournalActivity : ComponentActivity() {

    companion object {
        private const val TAG = "JournalActivity"
    }

    private val viewModel: JournalViewModel by viewModels {
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        val db = DatabaseProvider.getDatabase(this)
        val achievementRepository = AchievementRepository(
            db.userStatsDao(),
            db.journalDao()
        )
        ViewModelFactory(JournalRepository(achievementRepository, firebaseUid) to DeckRepository())
    }

    /**
     * Called when the activity is starting. Checks user session credentials, parses intent actions,
     * and sets up Compose UI components.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting JournalActivity")

        // Retrieve firebaseUid
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        Log.d(TAG, "Extracted firebase_uid: '$firebaseUid'")

        if (firebaseUid.isEmpty()) {
            Log.w(TAG, "firebase_uid is empty. Redirecting to MainActivity.")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Extract action parameter
        val action = intent.getStringExtra("action")
        Log.d(TAG, "Intent action parameter: '$action'")

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                var showAddDialog by remember { 
                    mutableStateOf(action == "NEW_ENTRY") 
                }
                var dateForNewEntry by remember { mutableStateOf(Calendar.getInstance()) }
                
                JournalCalendarScreen(
                    viewModel = viewModel,
                    onBack = {
                        Log.d(TAG, "Back button pressed in JournalCalendarScreen")
                        finish()
                    },
                    onAddEntry = { selectedDate ->
                        Log.d(TAG, "Add entry requested for date: ${selectedDate.time}")
                        dateForNewEntry = selectedDate
                        showAddDialog = true
                    },
                    onNavigate = { screen ->
                        Log.i(TAG, "Navigation requested to screen: '$screen'")
                        when (screen) {
                            "home" -> startActivity(Intent(this, IVU::class.java))
                            "decks" -> startActivity(Intent(this, DecksActivity::class.java))
                            "journal" -> Log.d(TAG, "Already on journal screen")
                            "profile" -> startActivity(Intent(this, AchievementsActivity::class.java))
                        }
                    }
                )
                
                if (showAddDialog) {
                    val state = viewModel.uiState.collectAsState().value
                    val decks = (state as? JournalUiState.Success)?.decks ?: emptyList()
                    
                    NewJournalEntryScreen(
                        initialDate = dateForNewEntry,
                        onDismiss = {
                            Log.d(TAG, "New journal entry dialog dismissed")
                            showAddDialog = false
                        },
                        decks = decks,
                        onConfirm = { date, mood, text, linkedDeckId ->
                            Log.i(TAG, "Creating journal entry - Mood: $mood, text length: ${text.length}, linkedDeckId: $linkedDeckId")
                            viewModel.createEntry(date, mood, text, linkedDeckId) { success, _ ->
                                if (success) {
                                    Log.i(TAG, "Journal entry successfully created")
                                    showAddDialog = false
                                } else {
                                    Log.e(TAG, "Failed to create journal entry")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * Called when the activity becomes visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: JournalActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: JournalActivity destroyed")
    }
}
