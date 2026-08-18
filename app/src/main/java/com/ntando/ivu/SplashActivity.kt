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
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Deck
import com.ntando.ivu.data.entity.Language
import com.ntando.ivu.data.entity.Flashcard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Ensure we have demo data for the "Decks" page
        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@SplashActivity)
            val existingDecks = db.deckDao().getDecksByUser(1).first() 
            
            if (existingDecks.isEmpty()) {
                Log.d(TAG, "Inserting demo data for user 1")
                val deck1 = db.deckDao().insertDeck(Deck(ownerId = 1, title = "Everyday isiZulu", language = Language.ZU, cardCount = 40))
                val deck2 = db.deckDao().insertDeck(Deck(ownerId = 1, title = "Afrikaans Basics", language = Language.AF, cardCount = 40))
                val deck3 = db.deckDao().insertDeck(Deck(ownerId = 1, title = "Exam Vocabulary", language = Language.EN, cardCount = 40))
                val deck4 = db.deckDao().insertDeck(Deck(ownerId = 1, title = "Travel Phrases", language = Language.ZU, cardCount = 40))
                
                // Add a sample card to the first deck so review works
                db.flashcardDao().insertFlashcard(Flashcard(deckId = deck1, frontText = "Sawubona", backText = "Hello"))
            }
        }

        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1L)

        if (currentUserId != -1L) {
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
