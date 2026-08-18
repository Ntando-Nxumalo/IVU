package com.ntando.ivu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPref = getSharedPreferences("IVUPrefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            finish()
            return
        }

        setupUI(currentUserId)
        setupNavigation()
    }

    private fun setupUI(userId: Long) {
        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnLogout = findViewById<android.view.View>(R.id.btnLogoutCard)

        val db = DatabaseProvider.getDatabase(this)
        lifecycleScope.launch {
            db.userDao().getUserById(userId).collect { user ->
                tvName.text = user?.name ?: "Ayanda Maseko"
                tvEmail.text = user?.email ?: "ayanda@example.com"
            }
        }

        btnLogout.setOnClickListener {
            val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
            sharedPref.edit { remove("current_user_id") }
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupNavigation() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, IVU::class.java))
            finish()
        }

        findViewById<android.view.View>(R.id.navDecks).setOnClickListener {
            startActivity(Intent(this, DecksActivity::class.java))
            finish()
        }

        findViewById<android.view.View>(R.id.navJournal).setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
            finish()
        }
    }
}
