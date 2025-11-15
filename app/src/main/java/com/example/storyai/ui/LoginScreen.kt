package com.example.storyai.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.storyai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = hiltViewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.backround),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // --- FIX: Add validation check ---
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.signIn(email, password,
                        onSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onFailure = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                // --- FIX: Add validation check ---
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.signUp(email, password,
                        onSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onFailure = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }) {
                Text("Sign Up")
            }
        }
    }
}
