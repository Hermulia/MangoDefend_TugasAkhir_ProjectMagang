package com.riset.mangodefendd.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.riset.mangodefendd.ui.viewmodel.AuthViewModel
import com.riset.mangodefendd.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    authViewModel.signInWithGoogle(token)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Muat profil saat pertama kali masuk atau saat status login berubah
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            viewModel.loadProfile()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHome,
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistory,
                    icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPricing,
                    icon = { Icon(Icons.Filled.Payments, contentDescription = "Pricing") },
                    label = { Text("Pricing") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "My Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image with Ring and Badge
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                primaryColor,
                                                Color.Transparent,
                                                primaryColor
                                            )
                                        ),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(105.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)
                            ) {
                                if (authState.isLoggedIn) {
                                    AsyncImage(
                                        model = uiState.profile?.photoUrl ?: authState.user?.photoUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.AccountCircle, 
                                        contentDescription = null, 
                                        modifier = Modifier.fillMaxSize(),
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                        
                        // Pro Badge
                        if (authState.isLoggedIn && uiState.activeSubscription != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.offset(y = 8.dp)
                            ) {
                                Text(
                                    text = uiState.activeSubscription?.plan?.planName?.uppercase() ?: "PRO",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (authState.isLoggedIn) (uiState.profile?.displayName ?: authState.user?.displayName ?: "User Name") else "Guest Mode",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (authState.isLoggedIn) (uiState.profile?.email ?: authState.user?.email ?: "user@email.com") else "Not logged in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Subscription",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Subscription Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF0F3D1F), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.activeSubscription?.plan?.planName ?: "Free Plan",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isActive = uiState.activeSubscription?.isActive == true
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.activeSubscription?.status?.uppercase() ?: "INACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        uiState.activeSubscription?.endDate?.let { endDate ->
                            Text(
                                text = "UNTIL: ${endDate.substringBefore("T")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = onNavigateToPricing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Manage", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onNavigateToTransactions,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("History", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login/Logout Button
            Button(
                onClick = { 
                    if (authState.isLoggedIn) {
                        authViewModel.signOut()
                        onLogout()
                    } else {
                        googleLauncher.launch(authViewModel.getSignInIntent())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (authState.isLoggedIn) Icons.Filled.ExitToApp else Icons.AutoMirrored.Filled.Login, 
                        contentDescription = null, 
                        tint = if (authState.isLoggedIn) Color.Red else MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (authState.isLoggedIn) "Logout" else "Login with Google", 
                        color = if (authState.isLoggedIn) Color.Red else MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
