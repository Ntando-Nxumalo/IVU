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
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.R
import kotlinx.coroutines.launch

/**
 * MainActivity serves as the Login screen for the IVU application.
 * Users can log in using their email/username.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Login screen")
        setContentView(R.layout.activity_main)

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
                            val sharedPref = getSharedPreferences("IVUPrefs", Context.MODE_PRIVATE)
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
            Toast.makeText(this, "Google sign-in coming soon!", Toast.LENGTH_SHORT).show()
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
}
