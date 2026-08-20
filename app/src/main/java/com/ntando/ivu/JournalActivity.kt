package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.ui.journal.JournalCalendarScreen
import com.ntando.ivu.ui.journal.NewJournalEntryDialog
import com.ntando.ivu.viewmodel.JournalViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import java.util.*

class JournalActivity : ComponentActivity() {

    private val viewModel: JournalViewModel by viewModels {
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)
        val db = DatabaseProvider.getDatabase(this)
        val achievementRepository = AchievementRepository(
            db.achievementDao(),
            db.userStatsDao(),
            db.journalDao(),
            db.flashcardDao()
        )
        ViewModelFactory(JournalRepository(achievementRepository, currentUserId))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            var showAddDialog by remember { mutableStateOf(false) }
            var dateForNewEntry by remember { mutableStateOf(Calendar.getInstance()) }
            
            JournalCalendarScreen(
                viewModel = viewModel,
                onBack = { finish() },
                onAddEntry = { selectedDate ->
                    dateForNewEntry = selectedDate
                    showAddDialog = true
                }
            )
            
            if (showAddDialog) {
                NewJournalEntryDialog(
                    initialDate = dateForNewEntry,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { date, mood, text, linkedDeckId ->
                        viewModel.createEntry(date, mood, text, linkedDeckId) { success ->
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
