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

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val TAG = "SplashActivity"
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved language preference
        val preferenceManager = PreferenceManager(this)
        lifecycleScope.launch {
            val savedLanguage = preferenceManager.appLanguage.first()
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(savedLanguage)
            AppCompatDelegate.setApplicationLocales(appLocale)
            
            // Apply theme
            val isDark = preferenceManager.isDarkTheme.first()
            if (isDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        setContentView(R.layout.activity_splash)

        val ivLogoContainer = findViewById<View>(R.id.ivLogoContainer)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        val tvFeatures = findViewById<TextView>(R.id.tvFeatures)
        val buttonContainer = findViewById<android.widget.LinearLayout>(R.id.buttonContainer)
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        val btnAlreadyHaveAccount = findViewById<Button>(R.id.btnAlreadyHaveAccount)

        buttonContainer.visibility = View.INVISIBLE

        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        ivLogoContainer.startAnimation(fadeIn)
        tvAppName.startAnimation(fadeIn)
        tvTagline.startAnimation(fadeIn)
        tvFeatures.startAnimation(fadeIn)

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1L)
        val firebaseUser = authRepository.getCurrentUser()
        val firebaseUid = firebaseUser?.uid

        // Ensure we have demo data and user stats
        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@SplashActivity)
            
            // Initialize UserStats if logged in
            if (firebaseUid != null) {
                val stats = db.userStatsDao().getUserStats(firebaseUid).first()
                if (stats == null) {
                    Log.d(TAG, "Initializing UserStats for $firebaseUid")
                    db.userStatsDao().insertOrUpdate(UserStats(userId = firebaseUid))
                }
            }

            // Ensure we have demo data for the "Decks" page
            val targetUserId = if (currentUserId != -1L) currentUserId else 1L
            val existingDecks = db.deckDao().getDecksByUser(targetUserId).first() 
            
            if (existingDecks.isEmpty()) {
                Log.d(TAG, "Inserting demo data for user $targetUserId")
                val deck1 = db.deckDao().insertDeck(Deck(ownerId = targetUserId, title = "Everyday isiZulu", language = Language.ZU, cardCount = 40))
                val deck2 = db.deckDao().insertDeck(Deck(ownerId = targetUserId, title = "Afrikaans Basics", language = Language.AF, cardCount = 40))
                val deck3 = db.deckDao().insertDeck(Deck(ownerId = targetUserId, title = "Exam Vocabulary", language = Language.EN, cardCount = 40))
                val deck4 = db.deckDao().insertDeck(Deck(ownerId = targetUserId, title = "Travel Phrases", language = Language.ZU, cardCount = 40))
                
                // Add a sample card to the first deck so review works
                db.flashcardDao().insertFlashcard(Flashcard(deckId = deck1, frontText = "Sawubona", backText = "Hello"))
            }
        }

        if (currentUserId != -1L || firebaseUser != null) {
            lifecycleScope.launch {
                delay(1500)
                startActivity(Intent(this@SplashActivity, IVU::class.java))
                finish()
            }
        } else {
            lifecycleScope.launch {
                delay(800)
                buttonContainer.visibility = View.VISIBLE
                buttonContainer.startAnimation(slideUp)
            }
        }

        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnAlreadyHaveAccount.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
