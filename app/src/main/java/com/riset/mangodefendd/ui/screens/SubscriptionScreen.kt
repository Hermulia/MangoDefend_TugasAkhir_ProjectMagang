package com.riset.mangodefendd.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.riset.mangodefendd.data.network.dto.PlanDto
import com.riset.mangodefendd.ui.viewmodel.AuthViewModel
import com.riset.mangodefendd.ui.viewmodel.SubscriptionViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SubscriptionScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    // Handle Toast for messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.checkoutSuccessMessage) {
        uiState.checkoutSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        containerColor = DashDarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = DashDarkBg,
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHome,
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DashGrey,
                        unselectedTextColor = DashGrey,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistory,
                    icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DashGrey,
                        unselectedTextColor = DashGrey,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Filled.Payments, contentDescription = "Pricing") },
                    label = { Text("Pricing") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DashGreen,
                        selectedTextColor = DashGreen,
                        unselectedIconColor = DashGrey,
                        unselectedTextColor = DashGrey,
                        indicatorColor = DashGreen.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DashGrey,
                        unselectedTextColor = DashGrey,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header (Same as Dashboard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = DashGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "MangoDefend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DashGreen
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = DashGrey)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DashCardBg)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (authState.isLoggedIn && authState.user?.photoUrl != null) {
                            AsyncImage(
                                model = authState.user?.photoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Filled.AccountCircle,
                                contentDescription = "Profile",
                                tint = DashGrey,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Page Title
            Text(
                text = "Upgrade Security",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unleash the full potential of MangoDefend\nwith operative-grade protocols.",
                style = MaterialTheme.typography.bodyMedium,
                color = DashGrey,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Plans List
            if (uiState.isLoading && uiState.plans.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DashGreen)
                }
            } else {
                uiState.plans.forEach { plan ->
                    if ((plan.price.toDoubleOrNull() ?: 0.0) > 0) {
                        PlanCardV2(
                            plan = plan,
                            onUpgradeClick = {
                                viewModel.checkoutPlan(planId = plan.id, method = "qris")
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Full screen loading overlay
        if (uiState.isLoading && uiState.plans.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DashGreen)
            }
        }
    }
}

@Composable
fun PlanCardV2(
    plan: PlanDto,
    onUpgradeClick: () -> Unit
) {
    val isBusiness = plan.planName.lowercase().contains("business") || plan.planName.lowercase().contains("bussiness")
    
    val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    val formattedPrice = try {
        formatRp.format(plan.price.toDouble())
    } catch (e: Exception) {
        "Rp ${plan.price}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DashCardBg)
            .border(1.dp, DashGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        if (isBusiness) {
            Surface(
                color = DashGreen,
                shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 20.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = "RECOMMENDED",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.planName.lowercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    color = DashGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${plan.durationDays} Days - ${plan.description ?: "Professional scale security"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val uploadLimitStr = if (plan.uploadFileLimit == 999) "Unlimited" else "${plan.uploadFileLimit} files/day"
            val fullScanLimitStr = if (plan.fullScanLimit == 999) "Unlimited" else "${plan.fullScanLimit} scans/day"

            Text(
                text = "Upload Limit: $uploadLimitStr",
                style = MaterialTheme.typography.bodyMedium,
                color = DashGrey
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Full Scan Limit: $fullScanLimitStr",
                style = MaterialTheme.typography.bodyMedium,
                color = DashGrey
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DashGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "UPGRADE TO ${plan.planName.uppercase()}",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
