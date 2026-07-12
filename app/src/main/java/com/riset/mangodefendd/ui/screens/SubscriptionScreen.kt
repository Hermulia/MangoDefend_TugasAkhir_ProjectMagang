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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
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

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
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
    
    var showLoginPrompt by remember { mutableStateOf(false) }

    // Back handler for WebView
    if (uiState.paymentUrl != null) {
        BackHandler {
            viewModel.onPaymentFinished()
        }
    }

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
            if (uiState.paymentUrl == null) {
                NavigationBar(
                    containerColor = DashDarkBg,
                    contentColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToHome,
                        icon = { Icon(Icons.Filled.Security, contentDescription = "Protect") },
                        label = { Text("Protect") },
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
        }
    ) { paddingValues ->
        if (uiState.paymentUrl != null) {
            // Midtrans Payment WebView
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TopAppBar(
                    title = { Text("Payment", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onPaymentFinished() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DashDarkBg)
                )
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: ""

                                    // CEGAT DISINI: Jika url mengandung domain dummy kita
                                    if (url.startsWith("https://mangodefend.app.close")) {
                                        // 1. Cek apakah ada aksi batal (action=back)
                                        if (url.contains("action=back")) {
                                            Toast.makeText(context, "Payment cancelled, please try again.", Toast.LENGTH_SHORT).show()
                                            viewModel.onPaymentFinished()
                                        }
                                        // 2. Jika sukses (settlement/capture)
                                        else if (url.contains("transaction_status=settlement") || 
                                                 url.contains("transaction_status=capture") ||
                                                 url.contains("transaction_status=pending")) {
                                            
                                            if (url.contains("transaction_status=pending")) {
                                                Toast.makeText(context, "Payment pending/processing.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Payment Successful!", Toast.LENGTH_SHORT).show()
                                            }
                                            
                                            viewModel.onPaymentFinished()
                                            onNavigateToHistory() // Arahkan ke halaman Riwayat
                                        }
                                        return true // Cegah webview meload URL dummy tersebut
                                    }
                                    return false // Izinkan URL midtrans berjalan normal
                                }
                            }
                            loadUrl(uiState.paymentUrl!!)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
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
                                isActive = uiState.activeSubscription?.planId == plan.id,
                                onUpgradeClick = { method ->
                                    if (authState.isLoggedIn) {
                                        viewModel.checkoutPlan(planId = plan.id, method = method)
                                    } else {
                                        showLoginPrompt = true
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showLoginPrompt) {
            AlertDialog(
                onDismissRequest = { showLoginPrompt = false },
                title = { Text("Feature Restricted") },
                text = { Text("Please login with your Google account to purchase or upgrade subscription plans.") },
                confirmButton = {
                    TextButton(onClick = {
                        showLoginPrompt = false
                        onNavigateToProfile()
                    }) {
                        Text("Login Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLoginPrompt = false }) {
                        Text("Cancel")
                    }
                }
            )
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
    isActive: Boolean = false,
    onUpgradeClick: (String) -> Unit
) {
    val isBusiness = plan.planName.lowercase().contains("business") || plan.planName.lowercase().contains("bussiness")
    var showMethodDialog by remember { mutableStateOf(false) }

    val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    val formattedPrice = try {
        formatRp.format(plan.price.toDouble())
    } catch (e: Exception) {
        "Rp ${plan.price}"
    }

    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = { Text("Select Payment Method") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("QRIS") },
                        leadingContent = { Icon(Icons.Default.QrCode, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showMethodDialog = false
                            onUpgradeClick("qris")
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Virtual Account (Bank Transfer)") },
                        leadingContent = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showMethodDialog = false
                            onUpgradeClick("virtual_account")
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DashCardBg)
            .border(
                width = if (isBusiness) 2.dp else 1.dp,
                brush = if (isBusiness) Brush.verticalGradient(listOf(DashGreen, DashGreen.copy(alpha = 0.3f))) else SolidColor(DashGreen.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (isBusiness) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = DashGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RECOMMENDED",
                        color = DashGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

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
                onClick = { if (!isActive) showMethodDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) DashGrey.copy(alpha = 0.3f) else DashGreen
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isActive
            ) {
                Text(
                    text = if (isActive) "CURRENT PLAN" else "UPGRADE TO ${plan.planName.uppercase()}",
                    color = if (isActive) Color.White else Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
