package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.ui.settings.SettingsScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.SettingsViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

/**
 * [SettingsActivity] allows users to configure application preferences.
 *
 * Features:
 * - Theme selection (Dark Mode vs Light Mode).
 * - Application language selection.
 * - Sign Out operation clearing local user sessions in [SharedPreferences] and Firebase.
 * - Navigation routing across IVU tabs.
 */
class SettingsActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private val authRepository = AuthRepository()
    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory(authRepository to PreferenceManager(this))
    }

    /**
     * Called when the activity is starting. Configures ViewModel and sets up Compose UI.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting SettingsActivity")

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = {
                        Log.d(TAG, "Back button pressed in SettingsScreen")
                        finish()
                    },
                    onSignOut = {
                        Log.i(TAG, "Sign Out action triggered. Clearing preferences session keys...")
                        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            remove("current_user_id")
                            remove("firebase_uid")
                            commit()
                        }
                        Log.i(TAG, "User session cleared. Redirecting to MainActivity...")
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onNavigate = { screen ->
                        Log.i(TAG, "Navigation requested to screen: '$screen'")
                        when (screen) {
                            "home" -> startActivity(Intent(this, IVU::class.java))
                            "decks" -> startActivity(Intent(this, DecksActivity::class.java))
                            "journal" -> startActivity(Intent(this, JournalActivity::class.java))
                            "profile" -> startActivity(Intent(this, AchievementsActivity::class.java))
                        }
                    }
                )
            }
        }
    }

    /**
     * Called when the activity becomes visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: SettingsActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: SettingsActivity destroyed")
    }
}
