package com.riset.mangodefendd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.riset.mangodefendd.data.network.dto.TransactionDto
import com.riset.mangodefendd.ui.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTransactionHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.transactions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.transactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No transactions yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.transactions) { transaction ->
                        TransactionItem(transaction)
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionDto) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    val formattedAmount = try {
        formatRp.format(transaction.amount.toDouble())
    } catch (e: Exception) {
        "Rp ${transaction.amount}"
    }

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
                    .background(
                        when (transaction.status.uppercase()) {
                            "SUCCESS", "COMPLETED" -> Color(0xFF0F3D1F)
                            "PENDING" -> Color(0xFF3D360F)
                            else -> Color(0xFF3D0F0F)
                        },
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = when (transaction.status.uppercase()) {
                        "SUCCESS", "COMPLETED" -> Color.Green
                        "PENDING" -> Color.Yellow
                        else -> Color.Red
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.plan?.planName ?: "Premium Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = transaction.createdAt?.substringBefore("T") ?: "Recent",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "Method: ${transaction.method ?: "Unknown"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (transaction.status.uppercase()) {
                        "SUCCESS", "COMPLETED" -> Color(0xFF0F3D1F)
                        "PENDING" -> Color(0xFF3D360F)
                        else -> Color(0xFF3D0F0F)
                    }
                ) {
                    Text(
                        text = transaction.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (transaction.status.uppercase()) {
                            "SUCCESS", "COMPLETED" -> Color.Green
                            "PENDING" -> Color.Yellow
                            else -> Color.Red
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
