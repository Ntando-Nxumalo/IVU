package com.ntando.ivu

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
import com.ntando.ivu.data.entity.User
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * MainActivity serves as the Login screen for the IVU application.
 * Users can log in using their email/username.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Login screen")
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val btnRegister = findViewById<TextView>(R.id.btnRegister)

        btnBack.setOnClickListener {
            finish()
        }

        /**
         * Login button click listener.
         * Validates credentials against the Room database.
         */
        btnLogin.setOnClickListener {
            val identifier = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (identifier.isNotEmpty() && password.isNotEmpty()) {
                Log.d(TAG, "Attempting login for: $identifier")
                lifecycleScope.launch {
                    val db = DatabaseProvider.getDatabase(this@MainActivity)
                    // Check if user exists by name or email
                    val user = db.userDao().getUserByName(identifier) ?: db.userDao().getUserByEmail(identifier)
                    
                    if (user != null) {
                        if (user.password == password) {
                            Log.i(TAG, "Login successful for user: ${user.name}")
                            // Store user ID in SharedPreferences for session persistence
                            val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putLong("current_user_id", user.id)
                                apply()
                            }

                            // Navigate to IVU (Main Hub)
                            val intent = Intent(this@MainActivity, IVU::class.java)
                            startActivity(intent)
                            finishAffinity() // Clear stack to prevent back-navigation to login
                        } else {
                            Log.w(TAG, "Login failed: Incorrect password")
                            Toast.makeText(this@MainActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w(TAG, "Login failed: User not found")
                        Toast.makeText(this@MainActivity, "User not found. Please register.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
            }
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        /**
         * Navigate to the Registration screen.
         */
        btnRegister.setOnClickListener {
            Log.d(TAG, "Navigating to RegisterActivity")
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
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
                    context = this@MainActivity,
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google Sign-In failed: ${e.message}")
                Toast.makeText(this@MainActivity, "Sign-in failed", Toast.LENGTH_SHORT).show()
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
                        Log.i(TAG, "Firebase Auth successful: ${firebaseUser?.email}")
                        
                        // Sync with local Room database
                        lifecycleScope.launch {
                            val db = DatabaseProvider.getDatabase(this@MainActivity)
                            var user = db.userDao().getUserByEmail(firebaseUser?.email ?: "")
                            
                            if (user == null) {
                                // Create local user if first time
                                val newUserId = db.userDao().insertUser(
                                    User(
                                        name = firebaseUser?.displayName ?: "Google User",
                                        email = firebaseUser?.email ?: "",
                                        password = "" // No local password for Google users
                                    )
                                )
                                user = db.userDao().getUserById(newUserId).firstOrNull()
                            }
                            
                            user?.let {
                                val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putLong("current_user_id", it.id)
                                    apply()
                                }
                                
                                val intent = Intent(this@MainActivity, IVU::class.java)
                                startActivity(intent)
                                finishAffinity()
                            }
                        }
                    } else {
                        Log.e(TAG, "Firebase Auth failed", task.exception)
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
