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
 *
 * Responsibilities:
 * - Session Validation & Recovery: Validates `current_user_id` and `firebase_uid`.
 *   Recovers session from Firebase email if local session key is missing.
 * - Dynamic Localization & Theme: Applies dark mode and selected language locale from [PreferenceManager].
 * - Dashboard UI Binding: Displays current date, user welcome banner, streak stats, and animated XP progress bar.
 * - Jetpack Compose Integration: Hosts [BottomNavigationBar] via a [ComposeView].
 */
class IVU : AppCompatActivity() {

    private val tag = "IVU"
    private var currentUserId: Long = -1

    /**
     * Called when the activity is starting. Configures app theme, language locale,
     * validates user authentication, inflates the layout, and initializes dashboard widgets.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate: Initializing Home screen")
        
        // Observe and apply theme preferences
        val preferenceManager = PreferenceManager(this)
        lifecycleScope.launch {
            preferenceManager.isDarkTheme.collect { isDark ->
                val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                if (AppCompatDelegate.getDefaultNightMode() != mode) {
                    Log.d(tag, "Applying night mode change: isDark=$isDark")
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }
        
        // Observe language preference and update application locales if changed
        lifecycleScope.launch {
            preferenceManager.appLanguage.collect { lang ->
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                if (currentLocales.toLanguageTags() != lang) {
                    Log.d(tag, "Applying locale change to language: $lang")
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
        }

        setContentView(R.layout.activity_ivu)

        // Retrieve saved user session keys
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", -1)
        val firebaseUid = sharedPref.getString("firebase_uid", null)
        Log.d(tag, "Session state - currentUserId: $currentUserId, firebaseUid: $firebaseUid")

        if (currentUserId == -1L) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                Log.i(tag, "Local session missing, attempting recovery for email: ${firebaseUser.email}")
                recoverSession(firebaseUser.email ?: "")
            } else {
                Log.w(tag, "Unauthenticated user detected in IVU home. Redirecting to MainActivity.")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            return
        }

        // If firebase_uid is missing but we have a session, try to populate it from FirebaseAuth
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

    /**
     * Called when the activity becomes visible to the user.
     */
    override fun onStart() {
        super.onStart()
        Log.d(tag, "onStart: IVU activity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy: IVU activity destroyed")
    }

    /**
     * Recovers a missing local user session by looking up the database record corresponding to [email].
     *
     * @param email User email address obtained from Firebase Auth.
     */
    private fun recoverSession(email: String) {
        Log.d(tag, "recoverSession: Looking up user by email $email")
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
                Log.i(tag, "Successfully recovered session for $email (userId: $currentUserId)")
                setupUI()
                setupNavigation()
            } else {
                Log.e(tag, "Session recovery failed: No user found in local DB for email $email. Signing out.")
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this@IVU, MainActivity::class.java))
                finish()
            }
        }
    }

    /**
     * Binds XML layout widgets, populates date string, queries user stats and updates streak/XP progress.
     */
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

        // Set Current Date formatted as "DayOfWeek, Day Month"
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        tvDate.text = sdf.format(Date())

        // Set Welcome header with user's first name
        lifecycleScope.launch {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val dbUser = db.userDao().getUserById(currentUserId).first()
            val nameToUse = firebaseUser?.displayName?.split(" ")?.firstOrNull() 
                ?: dbUser?.name?.split(" ")?.firstOrNull()
                ?: "Learner"
            
            Log.d(tag, "Setting header title for learner name: '$nameToUse'")
            tvHeaderTitle.text = getString(R.string.welcome_learner, nameToUse)
        }

        // Observe user stats live updates
        if (firebaseUid != null) {
            lifecycleScope.launch {
                db.userStatsDao().getUserStats(firebaseUid).collect { stats ->
                    if (stats == null) {
                        Log.w(tag, "setupUI: UserStats missing for $firebaseUid, initializing default record...")
                        db.userStatsDao().insertOrUpdate(com.ntando.ivu.data.entity.UserStats(userId = firebaseUid))
                    } else {
                        Log.d(tag, "setupUI: Stats updated - Streak: ${stats.currentStreak}, XP: ${stats.xp}")
                        
                        // Check if it's a new day to reset daily progress in UI
                        val sdfDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                        val isSameDay = stats.lastActiveDate != 0L && 
                                sdfDay.format(Date()) == sdfDay.format(Date(stats.lastActiveDate))
                        
                        val displayDailyReviews = if (isSameDay) stats.dailyReviews else 0

                        // Update UI widgets on main thread
                        runOnUiThread {
                            tvStreakTitle.text = getString(R.string.streak_format, stats.currentStreak)
                            tvXpPoints.text = getString(R.string.xp_earned_format, stats.xp)

                            val goal = 10
                            tvGoalProgress.text = getString(R.string.cards_reviewed_format, displayDailyReviews, goal)

                            pbXp.max = goal
                            ObjectAnimator.ofInt(pbXp, "progress", displayDailyReviews.coerceAtMost(goal))
                                .setDuration(1000)
                                .start()
                        }
                    }
                }
            }
        } else {
            Log.w(tag, "setupUI: firebaseUid is null, skipping userStats observation")
        }
    }

    /**
     * Binds click listeners to dashboard cards and sets up the Compose bottom navigation bar.
     */
    private fun setupNavigation() {
        Log.d(tag, "setupNavigation: Wiring up click listeners and bottom bar")

        // Study Decks card click
        findViewById<View>(R.id.cardStudy).setOnClickListener {
            Log.i(tag, "Dashboard cardStudy clicked -> Launching DecksActivity")
            startActivity(Intent(this, DecksActivity::class.java))
        }

        // AI Assistant card click
        findViewById<View>(R.id.cardAi).setOnClickListener {
            Log.i(tag, "Dashboard cardAi clicked -> Launching ChatActivity")
            startActivity(Intent(this, ChatActivity::class.java))
        }

        // Quick Journal entry card click
        findViewById<View>(R.id.cardJournal).setOnClickListener {
            Log.i(tag, "Dashboard cardJournal clicked -> Launching JournalActivity with action NEW_ENTRY")
            val intent = Intent(this, JournalActivity::class.java).apply {
                putExtra("action", "NEW_ENTRY")
            }
            startActivity(intent)
        }

        // Progress / Journal card click
        findViewById<View>(R.id.cardProgress).setOnClickListener {
            Log.i(tag, "Dashboard cardProgress clicked -> Launching JournalActivity")
            startActivity(Intent(this, JournalActivity::class.java))
        }

        // Settings button click
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            Log.i(tag, "Settings button clicked -> Launching SettingsActivity")
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Bottom Navigation Bar embedded with Jetpack Compose
        val composeBottomNav = findViewById<ComposeView>(R.id.composeBottomNav)
        val preferenceManager = PreferenceManager(this)
        composeBottomNav.setContent {
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)
            IVUTheme(darkTheme = isDarkTheme) {
                BottomNavigationBar(
                    currentScreen = "home",
                    onNavigate = { screen ->
                        Log.i(tag, "Bottom navigation item clicked: '$screen'")
                        when (screen) {
                            "home" -> Log.d(tag, "Already on home screen")
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
