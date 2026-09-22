package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.entity.Achievement
import com.ntando.ivu.data.entity.User
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.ui.auth.RegisterScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.RegisterViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * [RegisterActivity] handles the creation of new user accounts for IVU.
 *
 * Responsibilities:
 * - Presents the account registration UI ([RegisterScreen]) in Jetpack Compose.
 * - Supports email/password registration and Google Sign-In authentication via [CredentialManager].
 * - Seeds default user achievements into Room database upon initial profile creation.
 * - Stores active user session details in [android.content.SharedPreferences] and navigates to [IVU] home.
 */
class RegisterActivity : ComponentActivity() {

    private val tag = "RegisterActivity"
    private val authRepository = AuthRepository()
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    
    private val viewModel: RegisterViewModel by viewModels {
        ViewModelFactory(authRepository)
    }

    /**
     * Initializes activity lifecycle, FirebaseAuth, CredentialManager, and Compose content view.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate: Starting RegisterActivity")
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        
        setContent {
            IVUTheme {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = { 
                        Log.d(tag, "onRegisterSuccess callback triggered")
                        handleSuccessfulRegistration() 
                    },
                    onNavigateToLogin = {
                        Log.i(tag, "Navigating from Register to MainActivity (Login)")
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onGoogleSignInClick = {
                        Log.i(tag, "Google Sign-In button clicked in Register screen")
                        signInWithGoogle()
                    }
                )
            }
        }
    }

    /**
     * Called when the activity becomes visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(tag, "onStart: RegisterActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy: RegisterActivity destroyed")
    }

    /**
     * Completes post-registration user creation logic.
     * Creates local database records (`User` and initial [Achievement] items) if not present,
     * updates session preferences, and routes user to [IVU].
     */
    private fun handleSuccessfulRegistration() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.e(tag, "handleSuccessfulRegistration: firebaseUser is NULL")
            Toast.makeText(this, "Registration state error", Toast.LENGTH_SHORT).show()
            return
        }

        val email = firebaseUser.email ?: ""
        val name = firebaseUser.displayName ?: "IVU Learner"
        Log.d(tag, "handleSuccessfulRegistration started for email: $email, name: $name")
        Toast.makeText(this, "Creating your profile...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val db = DatabaseProvider.getDatabase(this@RegisterActivity)
                
                // Check if local user already exists
                val existingUser = db.userDao().getUserByEmail(email)
                val userId = if (existingUser == null) {
                    Log.i(tag, "Creating new local user record and initial achievements for: $email")
                    val newId = db.userDao().insertUser(
                        User(name = name, email = email, password = "")
                    )
                    
                    // Initialize achievements for new user
                    val firebaseUid = firebaseUser.uid
                    val initialAchievements = listOf(
                        Achievement(userId = firebaseUid, title = "Quick Starter", description = "Create your first study deck", icon = "bolt"),
                        Achievement(userId = firebaseUid, title = "7-Day Streak", description = "Study flashcards for 7 consecutive days", icon = "star"),
                        Achievement(userId = firebaseUid, title = "Card Master", description = "Master 100 flashcards", icon = "school"),
                        Achievement(userId = firebaseUid, title = "Journalist", description = "Write 5 journal entries about your progress", icon = "edit")
                    )
                    initialAchievements.forEach { db.achievementDao().insertAchievement(it) }
                    Log.d(tag, "Inserted ${initialAchievements.size} default achievements for UID $firebaseUid")
                    newId
                } else {
                    Log.d(tag, "Existing local user found with ID: ${existingUser.id}")
                    existingUser.id
                }

                // Save session preferences
                val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                val isSaved = with(sharedPref.edit()) {
                    putLong("current_user_id", userId)
                    putString("firebase_uid", firebaseUser.uid)
                    commit()
                }
                Log.i(tag, "Registration session saved successfully: $isSaved. Redirecting to IVU Home...")
                
                val intent = Intent(this@RegisterActivity, IVU::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e(tag, "Failed to complete local registration and achievement seeding", e)
                Toast.makeText(this@RegisterActivity, "Account created, but local sync failed. Please log in.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    /**
     * Initiates Google Sign-In request flow via [CredentialManager].
     */
    private fun signInWithGoogle() {
        Log.d(tag, "signInWithGoogle: Building Google ID credential request")
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
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
                Log.d(tag, "Credential manager returned credential result successfully")
                handleGoogleSignInResult(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(tag, "Google Sign-In failed with exception: ${e.message}")
                Toast.makeText(this@RegisterActivity, "Google Sign-In cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Processes Google credential result and authenticates with Firebase Auth.
     *
     * @param credential The credential returned from Google Sign-In flow.
     */
    private fun handleGoogleSignInResult(credential: androidx.credentials.Credential) {
        Log.d(tag, "handleGoogleSignInResult: credential type = ${credential.type}")
        val googleIdTokenCredential = try {
            when (credential) {
                is GoogleIdTokenCredential -> credential
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(tag, "Failed to parse Google ID Token credential", e)
            null
        }

        if (googleIdTokenCredential != null) {
            val googleIdToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            
            auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.i(tag, "Firebase Auth with Google (Register): Success")
                        handleSuccessfulRegistration()
                    } else {
                        Log.e(tag, "Firebase Auth with Google (Register): FAILED", task.exception)
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Log.e(tag, "handleGoogleSignInResult: Unexpected credential type")
            Toast.makeText(this, "Google Sign-In failed: Incorrect credential type", Toast.LENGTH_SHORT).show()
        }
    }
}
