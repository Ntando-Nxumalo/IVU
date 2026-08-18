package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * IVU main study hub (Home Dashboard).
 */
class IVU : AppCompatActivity() {

    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ivu)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupUI()
        setupNavigation()
    }

    private fun setupUI() {
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val db = DatabaseProvider.getDatabase(this)

        // Set Current Date
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        tvDate.text = sdf.format(Date())

        lifecycleScope.launch {
            db.userDao().getUserById(currentUserId).collect { user ->
                val name = user?.name?.split(" ")?.firstOrNull() ?: "Learner"
                tvHeaderTitle.text = "Sawubona, $name"
            }
        }
    }

    private fun setupNavigation() {
        // Dashboard cards
        findViewById<View>(R.id.cardStudy).setOnClickListener {
            startActivity(Intent(this, DecksActivity::class.java))
        }

        findViewById<View>(R.id.cardAi).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        findViewById<View>(R.id.cardJournal).setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
        }

        findViewById<View>(R.id.cardProgress).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }

        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Bottom Navigation
        findViewById<View>(R.id.navHome).setOnClickListener {
            // Already here
        }
        findViewById<View>(R.id.navDecks).setOnClickListener {
            startActivity(Intent(this, DecksActivity::class.java))
        }
        findViewById<View>(R.id.navJournal).setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
        }
        findViewById<View>(R.id.navMe).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }
    }
}
