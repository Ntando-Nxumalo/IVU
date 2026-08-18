package com.ntando.ivu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Achievement
import com.ntando.ivu.data.entity.User
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * RegisterActivity handles the creation of new user accounts for IVU.
 */
class RegisterActivity : AppCompatActivity() {

    private val tag = "RegisterActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etUsername = findViewById<EditText>(R.id.etUsername) // Used for Full Name
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        btnBack.setOnClickListener {
            finish()
        }

        registerBtn.setOnClickListener {
            val fullName = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = DatabaseProvider.getDatabase(this@RegisterActivity)
                val existingUser = db.userDao().getUserByEmail(email)
                
                if (existingUser != null) {
                    Toast.makeText(this@RegisterActivity, "User with this email already exists", Toast.LENGTH_SHORT).show()
                } else {
                    val userId = db.userDao().insertUser(User(name = fullName, email = email, password = password))
                    
                    // Initialize achievements
                    val initialAchievements = listOf(
                        Achievement(userId = userId, title = "Quick Starter", description = "Create your first study deck", icon = "bolt"),
                        Achievement(userId = userId, title = "7-Day Streak", description = "Study flashcards for 7 consecutive days", icon = "star"),
                        Achievement(userId = userId, title = "Card Master", description = "Master 100 flashcards", icon = "school"),
                        Achievement(userId = userId, title = "Journalist", description = "Write 5 journal entries about your progress", icon = "edit")
                    )
                    
                    initialAchievements.forEach {
                        db.achievementDao().insertAchievement(it)
                    }

                    // Save session
                    val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putLong("current_user_id", userId)
                        apply()
                    }

                    Toast.makeText(this@RegisterActivity, "Welcome to IVU, $fullName!", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to Home Dashboard
                    val intent = Intent(this@RegisterActivity, IVU::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }

        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        loginLink.setOnClickListener { 
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun signInWithGoogle() {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts = false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@RegisterActivity,
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(tag, "Google Sign-In failed: ${e.message}")
                Toast.makeText(this@RegisterActivity, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignIn(credential: androidx.credentials.Credential) {
        if (credential is GoogleIdTokenCredential) {
            val googleIdToken = credential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            
            auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        Log.i(tag, "Firebase Auth successful: ${firebaseUser?.email}")
                        
                        lifecycleScope.launch {
                            val db = DatabaseProvider.getDatabase(this@RegisterActivity)
                            var localUser = db.userDao().getUserByEmail(firebaseUser?.email ?: "")
                            
                            if (localUser == null) {
                                val userId = db.userDao().insertUser(
                                    User(
                                        name = firebaseUser?.displayName ?: "Google User",
                                        email = firebaseUser?.email ?: "",
                                        password = ""
                                    )
                                )
                                
                                // Initialize achievements for new user
                                val initialAchievements = listOf(
                                    Achievement(userId = userId, title = "Quick Starter", description = "Create your first study deck", icon = "bolt"),
                                    Achievement(userId = userId, title = "7-Day Streak", description = "Study flashcards for 7 consecutive days", icon = "star"),
                                    Achievement(userId = userId, title = "Card Master", description = "Master 100 flashcards", icon = "school"),
                                    Achievement(userId = userId, title = "Journalist", description = "Write 5 journal entries about your progress", icon = "edit")
                                )
                                initialAchievements.forEach { db.achievementDao().insertAchievement(it) }
                                
                                localUser = db.userDao().getUserById(userId).firstOrNull()
                            }
                            
                            localUser?.let {
                                val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putLong("current_user_id", it.id)
                                    apply()
                                }
                                
                                val intent = Intent(this@RegisterActivity, IVU::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                    } else {
                        Log.e(tag, "Firebase Auth failed", task.exception)
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
