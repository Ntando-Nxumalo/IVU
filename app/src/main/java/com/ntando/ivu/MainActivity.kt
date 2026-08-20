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
import com.ntando.ivu.data.entity.User
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.ui.auth.LoginScreen
import com.ntando.ivu.viewmodel.LoginViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * MainActivity serves as the Login screen for the IVU application.
 */
class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private val authRepository = AuthRepository()

    private val viewModel: LoginViewModel by viewModels {
        ViewModelFactory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { 
                    Log.d(tag, "onLoginSuccess (Success State) triggered")
                    handleSuccessfulAuth() 
                },
                onNavigateToRegister = {
                    startActivity(Intent(this, RegisterActivity::class.java))
                },
                onGoogleSignInClick = { signInWithGoogle() }
            )
        }
    }

    private fun handleSuccessfulAuth() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.e(tag, "handleSuccessfulAuth: firebaseUser is NULL")
            Toast.makeText(this, "Authentication state error", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            return
        }

        val email = firebaseUser.email ?: ""
        Log.d(tag, "handleSuccessfulAuth for: $email")
        Toast.makeText(this, "Syncing your profile...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val db = DatabaseProvider.getDatabase(this@MainActivity)
                var user = db.userDao().getUserByEmail(email)
                
                if (user == null) {
                    Log.d(tag, "User $email not found in local DB, creating new profile...")
                    val newUserId = db.userDao().insertUser(
                        User(
                            name = firebaseUser.displayName ?: "IVU Learner",
                            email = email,
                            password = "" 
                        )
                    )
                    user = User(id = newUserId, name = firebaseUser.displayName ?: "IVU Learner", email = email)
                }
                
                user.let {
                    Log.d(tag, "Saving session to SharedPreferences for user ID: ${it.id}")
                    val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                    val isSaved = with(sharedPref.edit()) {
                        putLong("current_user_id", it.id)
                        commit()
                    }
                    Log.d(tag, "Session saved: $isSaved. Redirecting to Home...")
                    Toast.makeText(this@MainActivity, "Welcome, ${it.name}!", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this@MainActivity, IVU::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to sync user with local database", e)
                Toast.makeText(this@MainActivity, "Database sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
        }
    }

    private fun signInWithGoogle() {
        Log.d(tag, "signInWithGoogle: Requesting credentials")
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
                    context = this@MainActivity,
                )
                Log.d(tag, "credentialManager.getCredential: Success")
                handleGoogleSignInResult(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(tag, "Google Sign-In failed: ${e.message}")
                viewModel.onSignInError("Google Sign-In cancelled or failed")
            }
        }
    }

    private fun handleGoogleSignInResult(credential: androidx.credentials.Credential) {
        Log.d(tag, "handleGoogleSignInResult: Type = ${credential.type}")
        
        val googleIdTokenCredential = try {
            when (credential) {
                is GoogleIdTokenCredential -> credential
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(tag, "Failed to parse Google ID Token", e)
            null
        }

        if (googleIdTokenCredential != null) {
            Log.d(tag, "handleGoogleSignInResult: Received ID Token")
            val googleIdToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            
            auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.i(tag, "Firebase Auth with Google: Success")
                        viewModel.onGoogleSignInSuccess()
                        // handleSuccessfulAuth() is called via LaunchedEffect in LoginScreen
                    } else {
                        Log.e(tag, "Firebase Auth with Google: FAILED", task.exception)
                        viewModel.onSignInError("Firebase authentication failed")
                    }
                }
        } else {
            Log.e(tag, "handleGoogleSignInResult: Unexpected or null credential: ${credential.type}")
            viewModel.onSignInError("Google Sign-In failed: Incorrect credential type")
        }
    }
}
