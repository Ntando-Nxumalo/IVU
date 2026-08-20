package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.ui.settings.SettingsScreen
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
            SettingsScreen(
                viewModel = viewModel,
                onBack = { finish() },
                onSignOut = {
                    val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        remove("current_user_id")
                        commit()
                    }
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}
