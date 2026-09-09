package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.ui.settings.SettingsScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.SettingsViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

class SettingsActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory(authRepository to PreferenceManager(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onSignOut = {
                        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            remove("current_user_id")
                            remove("firebase_uid")
                            commit()
                        }
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onNavigate = { screen ->
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
}
