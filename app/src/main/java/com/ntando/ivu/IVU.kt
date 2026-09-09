package com.ntando.ivu

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ntando.ivu.ui.components.BottomNavigationBar
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.User
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * IVU main study hub (Home Dashboard).
 * This activity serves as the central navigation point after successful authentication.
 * It displays real-time user statistics including streaks, XP, and study progress.
 */
class IVU : AppCompatActivity() {

    private val tag = "IVU"
    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate: Initializing Home screen")
        
        // Observe and apply theme
        val preferenceManager = PreferenceManager(this)
        lifecycleScope.launch {
            preferenceManager.isDarkTheme.collect { isDark ->
                val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                if (AppCompatDelegate.getDefaultNightMode() != mode) {
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }
        
        // Observe language and recreate if needed
        lifecycleScope.launch {
            preferenceManager.appLanguage.collect { lang ->
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                if (currentLocales.toLanguageTags() != lang) {
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
        }

        setContentView(R.layout.activity_ivu)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)
        val firebaseUid = sharedPref.getString("firebase_uid", null)

        if (currentUserId == -1L) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                // Recover session from Firebase email
                recoverSession(firebaseUser.email ?: "")
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            return
        }

        // If firebase_uid is missing but we have a session, try to fill it
        if (firebaseUid == null) {
            FirebaseAuth.getInstance().currentUser?.let { user ->
                with(sharedPref.edit()) {
                    putString("firebase_uid", user.uid)
                    commit()
                }
                Log.d(tag, "Populated missing firebase_uid: ${user.uid}")
            }
        }

        setupUI()
        setupNavigation()
    }

    private fun recoverSession(email: String) {
        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@IVU)
            val user = db.userDao().getUserByEmail(email)
            if (user != null) {
                currentUserId = user.id
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putLong("current_user_id", currentUserId)
                    firebaseUser?.let { putString("firebase_uid", it.uid) }
                    commit()
                }
                Log.d(tag, "Recovered session for $email")
                setupUI()
                setupNavigation()
            } else {
                // Firebase logged in but no local user? Redirect to login to trigger sync
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this@IVU, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun setupUI() {
        Log.d(tag, "setupUI: Initializing components and observers")
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvStreakTitle = findViewById<TextView>(R.id.tvStreakTitle)
        val tvXpPoints = findViewById<TextView>(R.id.tvXpPoints)
        val pbXp = findViewById<ProgressBar>(R.id.pbXp)
        val tvGoalProgress = findViewById<TextView>(R.id.tvGoalProgress)

        val db = DatabaseProvider.getDatabase(this)
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        
        // Always get the latest firebaseUid inside the setup method
        val firebaseUid = sharedPref.getString("firebase_uid", null) ?: FirebaseAuth.getInstance().currentUser?.uid

        // Set Current Date
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        tvDate.text = sdf.format(Date())

        lifecycleScope.launch {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val dbUser = db.userDao().getUserById(currentUserId).first()
            val nameToUse = firebaseUser?.displayName?.split(" ")?.firstOrNull() 
                ?: dbUser?.name?.split(" ")?.firstOrNull()
                ?: "Learner"
            
            tvHeaderTitle.text = getString(R.string.welcome_learner, nameToUse)
        }

        if (firebaseUid != null) {
            lifecycleScope.launch {
                db.userStatsDao().getUserStats(firebaseUid).collect { stats ->
                    if (stats == null) {
                        Log.w(tag, "setupUI: UserStats missing for $firebaseUid, initializing...")
                        // Initialize default stats if missing
                        db.userStatsDao().insertOrUpdate(com.ntando.ivu.data.entity.UserStats(userId = firebaseUid))
                    } else {
                        Log.d(tag, "setupUI: Stats updated - Streak: ${stats.currentStreak}, XP: ${stats.xp}")
                        // Update UI on main thread
                        runOnUiThread {
                            tvStreakTitle.text = resources.getQuantityString(R.plurals.days_format, stats.currentStreak, stats.currentStreak)
                            tvXpPoints.text = getString(R.string.xp_earned_format, stats.xp)

                            val goal = 10
                            tvGoalProgress.text = getString(R.string.cards_reviewed_format, stats.dailyReviews, goal)

                            pbXp.max = goal
                            ObjectAnimator.ofInt(pbXp, "progress", stats.dailyReviews.coerceAtMost(goal))
                                .setDuration(1000)
                                .start()
                        }
                    }
                }
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
            val intent = Intent(this, JournalActivity::class.java).apply {
                putExtra("action", "NEW_ENTRY")
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.cardProgress).setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
        }

        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Bottom Navigation (Compose)
        val composeBottomNav = findViewById<ComposeView>(R.id.composeBottomNav)
        val preferenceManager = PreferenceManager(this)
        composeBottomNav.setContent {
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)
            IVUTheme(darkTheme = isDarkTheme) {
                BottomNavigationBar(
                    currentScreen = "home",
                    onNavigate = { screen ->
                        when (screen) {
                            "home" -> {} // Already here
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
