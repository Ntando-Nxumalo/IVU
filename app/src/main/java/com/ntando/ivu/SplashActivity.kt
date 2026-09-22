package com.ntando.ivu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Deck
import com.ntando.ivu.data.entity.Language
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.data.entity.UserStats
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * [SplashActivity] serves as the initial entry point and splash landing screen for IVU.
 *
 * Responsibilities:
 * - Preference Application: Immediately applies stored theme and language preferences before layout inflation.
 * - Data Initialization: Ensures initial `UserStats` and demo deck/flashcard data exist in Room DB.
 * - Session Routing: Automatically routes logged-in users directly to [IVU] home after a delay;
 *   otherwise displays welcoming branding and options to Get Started ([RegisterActivity]) or Sign In ([MainActivity]).
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val TAG = "SplashActivity"
    private val authRepository = AuthRepository()

    /**
     * Called when the activity is starting. Configures initial app locale & theme,
     * seeds initial database records if necessary, animates splash elements, and executes session routing.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing SplashActivity")
        
        // Apply saved language & theme preferences early in lifecycle
        val preferenceManager = PreferenceManager(this)
        lifecycleScope.launch {
            val savedLanguage = preferenceManager.appLanguage.first()
            Log.d(TAG, "Applying saved language preference: '$savedLanguage'")
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(savedLanguage)
            AppCompatDelegate.setApplicationLocales(appLocale)
            
            // Apply theme
            val isDark = preferenceManager.isDarkTheme.first()
            Log.d(TAG, "Applying saved theme preference: isDark=$isDark")
            if (isDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        setContentView(R.layout.activity_splash)

        // Bind layout views
        val ivLogoContainer = findViewById<View>(R.id.ivLogoContainer)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        val tvFeatures = findViewById<TextView>(R.id.tvFeatures)
        val buttonContainer = findViewById<android.widget.LinearLayout>(R.id.buttonContainer)
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        val btnAlreadyHaveAccount = findViewById<Button>(R.id.btnAlreadyHaveAccount)

        buttonContainer.visibility = View.INVISIBLE

        // Load and trigger animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        ivLogoContainer.startAnimation(fadeIn)
        tvAppName.startAnimation(fadeIn)
        tvTagline.startAnimation(fadeIn)
        tvFeatures.startAnimation(fadeIn)

        // Extract session info
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1L)
        val firebaseUser = authRepository.getCurrentUser()
        val firebaseUid = firebaseUser?.uid
        Log.d(TAG, "Session status - currentUserId: $currentUserId, firebaseUid: $firebaseUid")

        // Ensure we have demo data and user stats initialized
        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@SplashActivity)
            
            // Initialize UserStats if logged in
            if (firebaseUid != null) {
                val stats = db.userStatsDao().getUserStats(firebaseUid).first()
                if (stats == null) {
                    Log.i(TAG, "Initializing UserStats record for $firebaseUid")
                    db.userStatsDao().insertOrUpdate(UserStats(userId = firebaseUid))
                }
            }

            // Ensure we have demo data for the "Decks" page
            val targetUserId = if (currentUserId != -1L) currentUserId else 1L
            val existingDecks = db.deckDao().getDecksByUser(targetUserId).first() 
            
            if (existingDecks.isEmpty()) {
                Log.i(TAG, "Inserting default demo decks and flashcard for user $targetUserId")
                val deck1 = db.deckDao().insertDeck(Deck(ownerId = targetUserId, title = "Everyday isiZulu", language = Language.ZU, cardCount = 40))
                val deck2 = db.deckDao().insertDeck(Deck(ownerId = targetUserId, title = "Afrikaans Basics", language = Language.AF, cardCount = 40))
                val deck3 = db.deckDao().insertDeck(Deck(ownerId = targetUserId, title = "Exam Vocabulary", language = Language.EN, cardCount = 40))
                val deck4 = db.deckDao().insertDeck(Deck(ownerId = targetUserId, title = "Travel Phrases", language = Language.ZU, cardCount = 40))
                
                // Add a sample card to the first deck so review works
                db.flashcardDao().insertFlashcard(Flashcard(deckId = deck1, frontText = "Sawubona", backText = "Hello"))
            }
        }

        // Decision logic for session routing vs onboarding view
        if (currentUserId != -1L || firebaseUser != null) {
            Log.i(TAG, "Active user session found. Scheduling navigation to IVU Home.")
            lifecycleScope.launch {
                delay(1500)
                startActivity(Intent(this@SplashActivity, IVU::class.java))
                finish()
            }
        } else {
            Log.i(TAG, "No active user session. Showing onboarding action buttons.")
            lifecycleScope.launch {
                delay(800)
                buttonContainer.visibility = View.VISIBLE
                buttonContainer.startAnimation(slideUp)
            }
        }

        // Get Started button action listener
        btnGetStarted.setOnClickListener {
            Log.i(TAG, "Get Started clicked -> Navigating to RegisterActivity")
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Already Have Account button action listener
        btnAlreadyHaveAccount.setOnClickListener {
            Log.i(TAG, "Already Have Account clicked -> Navigating to MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    /**
     * Called when the activity becomes visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: SplashActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: SplashActivity destroyed")
    }
}
