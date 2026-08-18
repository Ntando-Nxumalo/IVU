package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Achievement
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.viewmodel.AchievementViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AchievementsActivity displays the learner's progress and badges.
 * Updated to match the "Your Progress" design.
 */
class AchievementsActivity : AppCompatActivity() {

    private lateinit var viewModel: AchievementViewModel
    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupViewModel()
        setupNavigation()
        observeProgress()
    }

    private fun setupViewModel() {
        val db = DatabaseProvider.getDatabase(this)
        val repository = AchievementRepository(db.achievementDao(), db.deckDao(), db.flashcardDao(), db.journalDao())
        
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AchievementViewModel(repository, currentUserId) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[AchievementViewModel::class.java]
    }

    private fun observeProgress() {
        lifecycleScope.launch {
            viewModel.achievements.collectLatest { list ->
                // Map badges to the UI
                if (list.size >= 4) {
                    setupBadge(findViewById(R.id.badge1), list[0])
                    setupBadge(findViewById(R.id.badge2), list[1])
                    setupBadge(findViewById(R.id.badge3), list[2])
                    setupBadge(findViewById(R.id.badge4), list[3])
                }
            }
        }
    }

    private fun setupBadge(badgeView: View, achievement: Achievement) {
        val icon = badgeView.findViewById<ImageView>(R.id.ivBadgeIcon)
        val title = badgeView.findViewById<TextView>(R.id.tvBadgeTitle)
        
        title.text = achievement.title
        
        // Icon color based on status
        if (achievement.isUnlocked) {
            icon.alpha = 1.0f
            // Pick a color based on title or type if available
            val color = when {
                achievement.title.contains("streak") -> R.drawable.circle_gold
                achievement.title.contains("cards") -> R.drawable.circle_teal
                achievement.title.contains("journal") -> R.drawable.circle_red
                else -> R.drawable.circle_gold
            }
            icon.setBackgroundResource(color)
        } else {
            icon.alpha = 0.3f
            icon.setBackgroundResource(R.drawable.circle_gold) // Default grayed out
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, IVU::class.java))
            finish()
        }
        findViewById<View>(R.id.navDecks).setOnClickListener {
            startActivity(Intent(this, DecksActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navJournal).setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
            finish()
        }
    }
}
