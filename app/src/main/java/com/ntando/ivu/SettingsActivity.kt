package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.ui.settings.SettingsScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.SettingsViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [SettingsActivity] allows users to configure application preferences.
 */
class SettingsActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private val authRepository = AuthRepository()
    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory(authRepository to PreferenceManager(this))
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.i(TAG, "POST_NOTIFICATIONS permission result on Settings: isGranted=$isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting SettingsActivity")

        checkNotificationPermission()

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
                        Log.i(TAG, "Sign Out action triggered. Deleting token, clearing Room database and preferences session keys...")
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                com.ntando.ivu.network.FcmTokenManager(this@SettingsActivity).deleteTokenOnSignOut()
                                com.ntando.ivu.data.database.DatabaseProvider.getDatabase(this@SettingsActivity).clearAllTables()
                                Log.i(TAG, "Room database tables cleared on sign out.")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to clear Room database on sign out", e)
                            }
                        }
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

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Prompting for POST_NOTIFICATIONS permission on Settings screen")
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: SettingsActivity visible")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: SettingsActivity destroyed")
    }
}
