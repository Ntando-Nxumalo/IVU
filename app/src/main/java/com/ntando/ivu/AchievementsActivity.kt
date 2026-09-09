package com.ntando.ivu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.ui.achievements.AchievementScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.AchievementViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory

class AchievementsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""

        if (firebaseUid.isEmpty()) {
            finish()
            return
        }

        val db = DatabaseProvider.getDatabase(this)
        val repository = AchievementRepository(
            db.userStatsDao(),
            db.journalDao()
        )

        val viewModel: AchievementViewModel by viewModels {
            ViewModelFactory(repository to firebaseUid)
        }

        setContent {
            IVUTheme {
                AchievementScreen(viewModel = viewModel)
            }
        }
    }
}
