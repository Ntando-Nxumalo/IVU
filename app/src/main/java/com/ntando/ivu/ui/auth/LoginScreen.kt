package com.ntando.ivu.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.viewmodel.LoginUiState
import com.ntando.ivu.viewmodel.LoginViewModel

private const val TAG = "LoginScreen"

/**
 * Composable screen for user authentication using email/password or Google Sign-In.
 *
 * Layout Structure:
 * - [Scaffold] with a [CenterAlignedTopAppBar] formatted with custom orange background.
 * - Column with centered content:
 *   1. Email input text field with rounded corner styling.
 *   2. Password input text field with password visual transformation.
 *   3. Dynamic error message / loading indicator display based on [LoginUiState].
 *   4. Login submit button invoking [LoginViewModel.loginWithEmail].
 *   5. Horizontal divider for alternative sign-in options.
 *   6. Google Sign-In button invoking [onGoogleSignInClick].
 *   7. Navigation text button redirecting to registration via [onNavigateToRegister].
 *
 * @param viewModel ViewModel handling login logic and UI state updates.
 * @param onLoginSuccess Callback triggered upon successful authentication.
 * @param onNavigateToRegister Callback to navigate to the Registration screen.
 * @param onGoogleSignInClick Callback to trigger Google One-Tap or OAuth sign-in flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        Log.d(TAG, "Observed LoginUiState change: $uiState")
        if (uiState is LoginUiState.Success) {
            Log.i(TAG, "Login successful. Triggering onLoginSuccess callback.")
            onLoginSuccess()
        } else if (uiState is LoginUiState.Error) {
            Log.w(TAG, "Login failed with error: ${(uiState as LoginUiState.Error).message}")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.welcome_back),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Top bar back navigation clicked")
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE88A68)
                )
            )
        },
        containerColor = Color(0xFFFFF8F0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.welcome_subtitle),
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Email
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.label_email),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("you@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.label_password),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("••••••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState is LoginUiState.Error) {
                Text(
                    text = (uiState as LoginUiState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState is LoginUiState.Loading) {
                CircularProgressIndicator(color = Color(0xFFE88A68))
            } else {
                Button(
                    onClick = { 
                        if (email.isNotBlank() && password.isNotBlank()) {
                            Log.i(TAG, "Attempting email login for: $email")
                            viewModel.loginWithEmail(email, password)
                        } else {
                            Log.w(TAG, "Login attempted with blank email or password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(stringResource(R.string.log_in), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp, color = Color.LightGray)
                Text(stringResource(R.string.or_divider), modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray, fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp, color = Color.LightGray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    Log.i(TAG, "Google Sign-In button clicked")
                    onGoogleSignInClick()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE88A68))),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE88A68))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.continue_with_google), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = {
                Log.d(TAG, "Navigating to registration screen")
                onNavigateToRegister()
            }) {
                Text(stringResource(R.string.new_here_register), color = Color(0xFFE88A68))
            }
        }
    }
}
