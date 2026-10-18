package com.ntando.ivu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
import com.ntando.ivu.network.FcmTokenManager
import com.ntando.ivu.ui.auth.LoginScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.LoginViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * [MainActivity] serves as the Login screen for the IVU application.
 */
class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private val authRepository = AuthRepository()

    private val viewModel: LoginViewModel by viewModels {
        ViewModelFactory(authRepository)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.i(tag, "POST_NOTIFICATIONS permission result: isGranted=$isGranted")
        navigateToHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate: Initializing MainActivity (Login)")
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        setContent {
            IVUTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { 
                        Log.d(tag, "onLoginSuccess callback triggered")
                        handleSuccessfulAuth() 
                    },
                    onNavigateToRegister = {
                        Log.i(tag, "Navigating from Login to RegisterActivity")
                        startActivity(Intent(this, RegisterActivity::class.java))
                    },
                    onGoogleSignInClick = {
                        Log.i(tag, "Google Sign-In button clicked")
                        signInWithGoogle()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(tag, "onStart: MainActivity visible")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "onDestroy: MainActivity destroyed")
    }

    private fun handleSuccessfulAuth() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.e(tag, "handleSuccessfulAuth error: firebaseUser is NULL")
            Toast.makeText(this, "Authentication state error", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            return
        }

        val email = firebaseUser.email ?: ""
        Log.d(tag, "handleSuccessfulAuth started for email: $email, UID: ${firebaseUser.uid}")
        Toast.makeText(this, "Syncing your profile...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val db = DatabaseProvider.getDatabase(this@MainActivity)
                var user = db.userDao().getUserByEmail(email)
                
                if (user == null) {
                    Log.i(tag, "User $email not found in local DB. Creating new profile record...")
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
                        putString("firebase_uid", firebaseUser.uid)
                        commit()
                    }
                    Log.i(tag, "Session saved successfully: $isSaved.")

                    // Fetch & register FCM push token (non-fatal)
                    FcmTokenManager(this@MainActivity).registerCurrentToken()

                    Toast.makeText(this@MainActivity, "Welcome, ${it.name}!", Toast.LENGTH_SHORT).show()
                    checkPermissionAndProceed()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to sync user with local database", e)
                Toast.makeText(this@MainActivity, "Database sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
        }
    }

    private fun checkPermissionAndProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(tag, "Requesting POST_NOTIFICATIONS permission on Android 13+")
                Toast.makeText(this, getString(R.string.notification_permission_explanation), Toast.LENGTH_LONG).show()
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        navigateToHome()
    }

    private fun navigateToHome() {
        Log.i(tag, "Redirecting to Home (IVU)...")
        val intent = Intent(this@MainActivity, IVU::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun signInWithGoogle() {
        Log.d(tag, "signInWithGoogle: Building GetGoogleIdOption request")
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                Log.d(tag, "Calling credentialManager.getCredential")
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity,
                )
                Log.d(tag, "credentialManager.getCredential: Received response")
                handleGoogleSignInResult(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(tag, "Google Sign-In failed with GetCredentialException", e)
                viewModel.onSignInError("Google Sign-In cancelled or failed")
            }
        }
    }

    private fun handleGoogleSignInResult(credential: androidx.credentials.Credential) {
        Log.d(tag, "handleGoogleSignInResult: Credential Type = ${credential.type}")
        
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
            Log.e(tag, "Failed to parse Google ID Token from credential data", e)
            null
        }

        if (googleIdTokenCredential != null) {
            Log.d(tag, "Successfully parsed GoogleIdTokenCredential. Authenticating with Firebase...")
            val googleIdToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            
            auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.i(tag, "Firebase Auth with Google credential successful")
                        viewModel.onGoogleSignInSuccess()
                    } else {
                        Log.e(tag, "Firebase Auth with Google FAILED", task.exception)
                        viewModel.onSignInError("Firebase authentication failed")
                    }
                }
        } else {
            Log.e(tag, "handleGoogleSignInResult: Unexpected or null credential type: ${credential.type}")
            viewModel.onSignInError("Google Sign-In failed: Incorrect credential type")
        }
    }
}
