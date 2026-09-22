package com.ntando.ivu.ui.auth

import android.util.Log
import android.util.Patterns
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
import com.ntando.ivu.viewmodel.RegisterUiState
import com.ntando.ivu.viewmodel.RegisterViewModel

private const val TAG = "RegisterScreen"

/**
 * Composable screen enabling new users to create an account using name, email, and password, or via Google Sign-In.
 *
 * Layout Structure:
 * - [Scaffold] with a [CenterAlignedTopAppBar] and back navigation button.
 * - Form column with validation:
 *   1. Full Name input text field with real-time field validation.
 *   2. Email input text field validated against pattern matching [Patterns.EMAIL_ADDRESS].
 *   3. Password input text field validated for minimum length (8 characters).
 *   4. Inline red error messages for failed validations or server errors.
 *   5. Registration submit button triggering [RegisterViewModel.registerUser].
 *   6. Google Sign-In alternative option.
 *   7. Navigation button redirecting to login via [onNavigateToLogin].
 *
 * @param viewModel ViewModel handling registration operations and state publishing.
 * @param onRegisterSuccess Callback executed when registration completes successfully.
 * @param onNavigateToLogin Callback navigating back to the Login screen.
 * @param onGoogleSignInClick Callback initiating Google authentication flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    val nameRequiredErr = stringResource(R.string.error_name_required)
    val invalidEmailErr = stringResource(R.string.error_invalid_email)
    val passwordLengthErr = stringResource(R.string.error_password_length)

    LaunchedEffect(uiState) {
        Log.d(TAG, "Observed RegisterUiState change: $uiState")
        if (uiState is RegisterUiState.Success) {
            Log.i(TAG, "Registration succeeded. Triggering onRegisterSuccess callback.")
            onRegisterSuccess()
        } else if (uiState is RegisterUiState.Error) {
            Log.w(TAG, "Registration failed with error: ${(uiState as RegisterUiState.Error).message}")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.create_account),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigating back to Login screen via top app bar icon")
                        onNavigateToLogin()
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
            Spacer(modifier = Modifier.height(32.dp))

            // Full Name
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.label_full_name),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = if (it.isBlank()) nameRequiredErr else null
                    },
                    placeholder = { Text(stringResource(R.string.hint_full_name)) },
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        errorContainerColor = Color.White
                    )
                )
                nameError?.let { Text(it, color = Color.Red, fontSize = 11.sp) }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    onValueChange = { 
                        email = it
                        emailError = if (!Patterns.EMAIL_ADDRESS.matcher(it).matches()) invalidEmailErr else null
                    },
                    placeholder = { Text("you@example.com") },
                    isError = emailError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        errorContainerColor = Color.White
                    )
                )
                emailError?.let { Text(it, color = Color.Red, fontSize = 11.sp) }
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
                    onValueChange = { 
                        password = it
                        passwordError = if (it.length < 8) passwordLengthErr else null
                    },
                    placeholder = { Text("••••••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        errorContainerColor = Color.White
                    )
                )
                passwordError?.let { Text(it, color = Color.Red, fontSize = 11.sp) }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is RegisterUiState.Error) {
                Text(
                    text = (uiState as RegisterUiState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState is RegisterUiState.Loading) {
                CircularProgressIndicator(color = Color(0xFFE88A68))
            } else {
                Button(
                    onClick = {
                        val isNameValid = name.isNotBlank()
                        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        val isPasswordValid = password.length >= 8
                        
                        if (!isNameValid) nameError = nameRequiredErr
                        if (!isEmailValid) emailError = invalidEmailErr
                        if (!isPasswordValid) passwordError = passwordLengthErr
                        
                        if (isNameValid && isEmailValid && isPasswordValid) {
                            Log.i(TAG, "Submitting registration request for name: $name, email: $email")
                            viewModel.registerUser(name, email, password)
                        } else {
                            Log.w(TAG, "Registration validation failed: nameValid=$isNameValid, emailValid=$isEmailValid, passwordValid=$isPasswordValid")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(stringResource(R.string.btn_create_account), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    Log.i(TAG, "Google Sign-In button clicked on RegisterScreen")
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
                Log.d(TAG, "Navigating to Login screen via text button")
                onNavigateToLogin()
            }) {
                Text(stringResource(R.string.already_have_account_login), color = Color(0xFFE88A68))
            }
        }
    }
}
