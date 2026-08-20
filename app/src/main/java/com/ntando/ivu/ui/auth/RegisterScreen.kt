package com.ntando.ivu.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
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
        if (uiState is RegisterUiState.Success) {
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.create_account),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3D2B1F)
        )
        Text(
            text = stringResource(R.string.join_ivu_msg),
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { 
                name = it
                nameError = if (it.isBlank()) nameRequiredErr else null
            },
            label = { Text(stringResource(R.string.label_full_name)) },
            isError = nameError != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        nameError?.let { Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start)) }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                emailError = if (!Patterns.EMAIL_ADDRESS.matcher(it).matches()) invalidEmailErr else null
            },
            label = { Text(stringResource(R.string.label_email)) },
            isError = emailError != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        emailError?.let { Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start)) }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                passwordError = if (it.length < 8) passwordLengthErr else null
            },
            label = { Text(stringResource(R.string.label_password)) },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        passwordError?.let { Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start)) }

        Spacer(modifier = Modifier.height(24.dp))

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
                        viewModel.registerUser(name, email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68)),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.register), fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.sign_up_with_google))
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.already_have_account), color = Color(0xFFE88A68))
        }
    }
}
