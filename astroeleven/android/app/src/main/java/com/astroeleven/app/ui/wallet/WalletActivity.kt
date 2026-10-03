package com.astroeleven.app.ui.wallet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.astroeleven.app.R
import com.astroeleven.app.data.api.ApiClient
import com.astroeleven.app.data.local.TokenManager
import com.astroeleven.app.ui.theme.CosmicAppTheme
import com.astroeleven.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.ArrayList

data class WalletRechargePack(
    val amount: Int,
    val extraPercent: Int,
    val extraText: String = if (extraPercent > 0) "Get $extraPercent% Extra" else "Get 0% Extra"
)

class WalletActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager
    private val transactionsState = mutableStateListOf<JSONObject>()
    private var balanceState by mutableDoubleStateOf(0.0)
    private var superBalanceState by mutableDoubleStateOf(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)

        // Set status bar to yellow with dark icons matching the top bar
        try {
            window.statusBarColor = android.graphics.Color.parseColor("#FFE600")
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        } catch (_: Exception) {}

        updateBalanceFromSession()

        setContent {
            CosmicAppTheme {
                WalletScreen(
                    balance = balanceState,
                    superBalance = superBalanceState,
                    transactions = transactionsState,
                    onAddMoney = { amount, promo ->
                        if (amount < 1) {
                            Toast.makeText(this, getString(R.string.enter_valid_amount), Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(this, com.astroeleven.app.ui.payment.PaymentActivity::class.java)
                            intent.putExtra("amount", amount.toDouble())
                            if (promo != null) {
                                intent.putExtra("promoCode", promo)
                            }
                            startActivity(intent)
                        }
                    },
                    onRefreshHistory = {
                        refreshWalletBalance()
                        loadPaymentHistory()
                    }
                )
            }
        }

        loadPaymentHistory()
    }

    override fun onResume() {
        super.onResume()
        refreshWalletBalance()
        loadPaymentHistory()

        com.astroeleven.app.data.remote.SocketManager.onWalletUpdate { data ->
            runOnUiThread {
                val newBalance = data.optDouble("balance", 0.0)
                val newSuperBalance = data.optDouble("superBalance", 0.0)
                tokenManager.updateWalletBalance(newBalance)
                tokenManager.updateSuperWalletBalance(newSuperBalance)
                balanceState = newBalance
                superBalanceState = newSuperBalance
            }
        }
    }

    override fun onPause() {
        super.onPause()
        com.astroeleven.app.data.remote.SocketManager.off("wallet-update")
    }

    private fun updateBalanceFromSession() {
        val user = tokenManager.getUserSession()
        balanceState = user?.walletBalance ?: 0.0
        superBalanceState = user?.superWalletBalance ?: 0.0
    }

    private fun refreshWalletBalance() {
        val userId = tokenManager.getUserSession()?.userId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.api.getUserProfile(userId)
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    runOnUiThread {
                        tokenManager.saveUserSession(user)
                        balanceState = user.walletBalance ?: 0.0
                        superBalanceState = user.superWalletBalance ?: 0.0
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadPaymentHistory() {
        val userId = tokenManager.getUserSession()?.userId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${Constants.SERVER_URL}/api/payment/history/$userId")
                    .get()
                    .build()

                val client = OkHttpClient()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        val json = JSONObject(body ?: "{}")
                        val data = json.optJSONArray("data")

                        val newTransactions = ArrayList<JSONObject>()
                        if (data != null) {
                            for (i in 0 until data.length()) {
                                newTransactions.add(data.getJSONObject(i))
                            }
                        }

                        runOnUiThread {
                            transactionsState.clear()
                            transactionsState.addAll(newTransactions)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    balance: Double,
    superBalance: Double = 0.0,
    transactions: List<JSONObject>,
    onAddMoney: (Int, String?) -> Unit,
    onRefreshHistory: () -> Unit
) {
    val context = LocalContext.current
    var customAmountInput by remember { mutableStateOf("") }

    // Color definitions matching the screenshot
    val yellowHeader = Color(0xFFFFDE03) // Bright Yellow Header
    val yellowBadge = Color(0xFFFFDE03)  // Bright Yellow Extra Offer Badge
    val cardBorder = Color(0xFFE5E7EB)

    // Exact packs shown in the screenshot
    val rechargePacks = remember {
        listOf(
            WalletRechargePack(50, 50, "Get 50% Extra"),
            WalletRechargePack(100, 100, "Get 100% Extra"),
            WalletRechargePack(500, 100, "Get 100% Extra"),
            WalletRechargePack(1000, 100, "Get 100% Extra"),
            WalletRechargePack(200, 10, "Get 10% Extra"),
            WalletRechargePack(5000, 20, "Get 20% Extra"),
            WalletRechargePack(2000, 100, "Get 100% Extra"),
            WalletRechargePack(20, 50, "Get 50% Extra"),
            WalletRechargePack(2, 0, "Get 0% Extra")
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add money to wallet",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF111827)
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF111827)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshHistory) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Refresh",
                            tint = Color(0xFF111827)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = yellowHeader,
                    titleContentColor = Color(0xFF111827),
                    navigationIconContentColor = Color(0xFF111827),
                    actionIconContentColor = Color(0xFF111827)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Available Total Balance Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = "Available Total Balance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF6B7280) // Muted Grey
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹ ${String.format(java.util.Locale.US, "%.1f", balance)}",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827) // Solid Black
                    )
                    if (superBalance > 0.0) {
                        Text(
                            text = "Super Wallet Bonus: ₹ ${superBalance.toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 2. 2-Column Recharge Packs Grid (Direct click-to-pay)
            val chunkedPacks = rechargePacks.chunked(2)
            items(chunkedPacks) { rowPacks ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (pack in rowPacks) {
                        Box(modifier = Modifier.weight(1f)) {
                            WalletPackCard(
                                pack = pack,
                                yellowColor = yellowBadge,
                                borderColor = cardBorder,
                                onClick = {
                                    val promo = if (pack.extraPercent > 0) "EXTRA${pack.extraPercent}" else null
                                    onAddMoney(pack.amount, promo)
                                }
                            )
                        }
                    }
                    if (rowPacks.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 3. Custom Amount Input Option
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Custom Recharge Amount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customAmountInput,
                                onValueChange = { customAmountInput = it.filter { c -> c.isDigit() } },
                                placeholder = { Text("Enter ₹ Amount", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = yellowHeader,
                                    unfocusedBorderColor = Color(0xFFD1D5DB)
                                )
                            )
                            Button(
                                onClick = {
                                    val amt = customAmountInput.toIntOrNull() ?: 0
                                    if (amt >= 1) {
                                        onAddMoney(amt, null)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.enter_valid_amount), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = yellowHeader),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(
                                    text = "Pay Now",
                                    color = Color(0xFF111827),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Recent Transactions History
            if (transactions.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.recent_transactions),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.padding(start = 20.dp, top = 26.dp, bottom = 8.dp)
                    )
                }

                items(transactions) { tx ->
                    val amt = tx.optDouble("amount", 0.0)
                    val status = tx.optString("status", "pending")
                    val date = tx.optString("createdAt", "").take(10)
                    val isSuccess = status == "success"

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF9FAFB),
                        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Rounded.AccountBalanceWallet else Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = if (isSuccess) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (isSuccess) "Recharge Success" else "Payment $status",
                                    color = Color(0xFF1F2937),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = date,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                            Text(
                                text = "₹${amt.toInt()}",
                                fontSize = 16.sp,
                                color = if (isSuccess) Color(0xFF15803D) else Color(0xFF374151),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletPackCard(
    pack: WalletRechargePack,
    yellowColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                clip = false
            ),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Half (White Background) - Amount
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "₹ ${pack.amount}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF111827)
                )
            }

            // Bottom Half (Yellow Background) - Extra Offer Tag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(yellowColor)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pack.extraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
