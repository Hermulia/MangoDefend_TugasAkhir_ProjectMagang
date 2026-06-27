package com.riset.mangodefendd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.riset.mangodefendd.ui.viewmodel.AuthViewModel
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    authViewModel.signInWithGoogle(token)
                }
            } catch (e: Exception) {
                // Ignore for now or handle via ViewModel state
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "MangoDefend",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            "Antivirus with Machine Learning",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Guest Mode
        Button(
            onClick = { 
                authViewModel.signOut() // Ensure data is cleared when entering guest mode
                onLoginSuccess() 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Lihat Fitur Dulu (Guest Mode)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In
        Button(
            onClick = {
                launcher.launch(authViewModel.getSignInIntent())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Sign in with Google")
        }

        if (authState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Error: ${authState.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (authState.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun AuthGuard(
    isLoggedIn: Boolean,
    content: @Composable () -> Unit
) {
    if (isLoggedIn) {
        content()
    } else {
        Text(
            "Please log in to access this feature",
            modifier = Modifier.padding(16.dp)
        )
    }
}

