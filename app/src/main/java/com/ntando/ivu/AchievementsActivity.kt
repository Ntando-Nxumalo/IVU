package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.ui.achievements.AchievementScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.AchievementViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

/**
 * [AchievementsActivity] displays the user's unlocked and locked achievements, streak statistics, and learning progress.
 *
 * Key Responsibilities:
 * - Session Management: Validates that a valid [firebaseUid] exists in [SharedPreferences] before displaying content.
 * - Jetpack Compose Integration: Hosts the [AchievementScreen] composable wrapped in the app's [IVUTheme].
 * - Navigation: Handles bottom bar navigation events to route between Home, Decks, Journal, and Profile.
 * - Dynamic Theme Application: Observes [PreferenceManager] to dynamically switch between dark and light themes.
 */
class AchievementsActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AchievementsActivity"
    }

    /**
     * Called when the activity is starting. Initializes dependencies, checks session validity,
     * builds the [AchievementViewModel], and sets up the Jetpack Compose UI content.
     *
     * @param savedInstanceState If non-null, this activity is being re-constructed from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing AchievementsActivity")

        // Retrieve user session credentials from SharedPreferences
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""
        Log.d(TAG, "Extracted firebase_uid from preferences: '$firebaseUid'")

        // If no user is logged in, terminate activity to prevent unauthorized access
        if (firebaseUid.isEmpty()) {
            Log.w(TAG, "firebase_uid is missing or empty. Finishing activity.")
            finish()
            return
        }

        // Initialize database and repository instances
        val db = DatabaseProvider.getDatabase(this)
        val repository = AchievementRepository(
            db.userStatsDao(),
            db.journalDao()
        )

        // Instantiate ViewModel using custom Factory with parameter binding
        val viewModel: AchievementViewModel by viewModels {
            ViewModelFactory(repository to firebaseUid)
        }

        // Set up Jetpack Compose UI layout
        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                AchievementScreen(
                    viewModel = viewModel,
                    onNavigate = { screen ->
                        Log.i(TAG, "Navigation requested to screen: '$screen'")
                        when (screen) {
                            "home" -> startActivity(Intent(this, IVU::class.java))
                            "decks" -> startActivity(Intent(this, DecksActivity::class.java))
                            "journal" -> startActivity(Intent(this, JournalActivity::class.java))
                            "profile" -> Log.d(TAG, "Already on profile/achievements screen")
                        }
                    }
                )
            }
        }
    }

    /**
     * Called when the activity becomes visible to the user.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: AchievementsActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: AchievementsActivity destroyed")
    }
}
