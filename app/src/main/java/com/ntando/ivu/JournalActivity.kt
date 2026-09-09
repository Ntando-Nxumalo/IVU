package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
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

class JournalActivity : ComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""

        if (firebaseUid.isEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                var showAddDialog by remember { 
                    mutableStateOf(intent.getStringExtra("action") == "NEW_ENTRY") 
                }
                var dateForNewEntry by remember { mutableStateOf(Calendar.getInstance()) }
                
                JournalCalendarScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onAddEntry = { selectedDate ->
                        dateForNewEntry = selectedDate
                        showAddDialog = true
                    },
                    onNavigate = { screen ->
                        when (screen) {
                            "home" -> startActivity(Intent(this, IVU::class.java))
                            "decks" -> startActivity(Intent(this, DecksActivity::class.java))
                            "journal" -> {} // Already here
                            "profile" -> startActivity(Intent(this, AchievementsActivity::class.java))
                        }
                    }
                )
                
                if (showAddDialog) {
                    val state = viewModel.uiState.collectAsState().value
                    val decks = (state as? JournalUiState.Success)?.decks ?: emptyList()
                    
                    NewJournalEntryScreen(
                        initialDate = dateForNewEntry,
                        onDismiss = { showAddDialog = false },
                        decks = decks,
                        onConfirm = { date, mood, text, linkedDeckId ->
                            viewModel.createEntry(date, mood, text, linkedDeckId) { success, _ ->
                                if (success) {
                                    showAddDialog = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
