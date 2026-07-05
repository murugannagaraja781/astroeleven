package com.astroeleven.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.astroeleven.app.R
import com.astroeleven.app.data.local.TokenManager
import com.astroeleven.app.data.repository.AuthRepository
import com.astroeleven.app.ui.theme.CosmicAppTheme
import com.astroeleven.app.ui.theme.AstroDimens
import com.astroeleven.app.utils.FcmTokenHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {

    private val repository = AuthRepository()
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        val phone = intent.getStringExtra("phone") ?: run {
            finish()
            return
        }
        val referralCode = intent.getStringExtra("referralCode")

        setContent {
            CosmicAppTheme {
                OtpScreen(
                    phone = phone,
                    onVerifyOtp = { otp -> verifyOtp(phone, otp, referralCode) },
                    onResendOtp = { resendOtp(phone) }
                )
            }
        }
    }

    private fun resendOtp(phone: String) {
        lifecycleScope.launch {
            try {
                val result = repository.sendOtp(phone)
                if (result.isSuccess) {
                    Toast.makeText(this@OtpVerificationActivity, "OTP Resent Successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@OtpVerificationActivity, "Failed to resend OTP. Try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@OtpVerificationActivity, "Network error. Check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyOtp(phone: String, otp: String, referralCode: String? = null) {
        if (otp.length != 4) {
            Toast.makeText(this, "Enter 4 digit OTP", Toast.LENGTH_SHORT).show()
            return
        }

        // Backdoors
        if (otp == "0009") {
            val intent = Intent(this, com.astroeleven.app.ui.admin.SuperPowerAdminDashboardActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        if (otp == "7777") {
            val dummyUser = com.astroeleven.app.data.model.AuthResponse(
                ok = true, userId = "dummy_client_001", name = "Test Client", role = "user", phone = "9999999999", walletBalance = 500.0, image = "", error = null
            )
            tokenManager.saveUserSession(dummyUser)
            FcmTokenHelper.registerFcmToken("dummy_client_001")
            startActivity(Intent(this, com.astroeleven.app.ui.home.HomeActivity::class.java))
            finishAffinity()
            return
        }

        lifecycleScope.launch {
            val result = repository.verifyOtp(phone, otp)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                tokenManager.saveUserSession(user)
                
                user.userId?.let { userId ->
                    FcmTokenHelper.registerFcmToken(userId)
                }
                
                Toast.makeText(this@OtpVerificationActivity, "Welcome ${user.name}", Toast.LENGTH_SHORT).show()
                val intent = when (user.role) {
                    "astrologer" -> Intent(this@OtpVerificationActivity, com.astroeleven.app.ui.astro.AstrologerDashboardActivity::class.java)
                    "admin" -> Intent(this@OtpVerificationActivity, com.astroeleven.app.ui.admin.SuperPowerAdminDashboardActivity::class.java)
                    else -> Intent(this@OtpVerificationActivity, com.astroeleven.app.ui.home.HomeActivity::class.java)
                }
                startActivity(intent)
                finishAffinity()
            } else {
                Toast.makeText(this@OtpVerificationActivity, result.exceptionOrNull()?.message ?: "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    phone: String,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Countdown timer for resend (60 seconds)
    var resendTimer by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    // Timer countdown logic
    LaunchedEffect(canResend) {
        if (!canResend) {
            resendTimer = 60
            while (resendTimer > 0) {
                delay(1000)
                resendTimer--
            }
            canResend = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicAppTheme.colors.bgStart),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AstroDimens.Large)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp), spotColor = CosmicAppTheme.colors.accent),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicAppTheme.colors.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(AstroDimens.Large)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verification Code",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CosmicAppTheme.colors.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "OTP sent to +${phone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicAppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    text = "Check your SMS inbox",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // OTP Input Boxes
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { focusRequester.requestFocus() }
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = otp,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                otp = it
                            }
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .matchParentSize()
                            .alpha(0f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                if (otp.length == 4) {
                                    onVerifyOtp(otp)
                                }
                            }
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) { index ->
                            val digit = if (index < otp.length) otp[index].toString() else ""
                            val isFocused = index == otp.length
                            OtpDigitBox(digit, isFocused)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                com.astroeleven.app.ui.theme.components.AstroButton(
                    text = "VERIFY OTP",
                    onClick = {
                        if (otp.length == 4) {
                            isLoading = true
                            onVerifyOtp(otp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Resend OTP Section
                if (canResend) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Didn't receive the OTP? ",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicAppTheme.colors.textSecondary
                        )
                        if (isResending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CosmicAppTheme.colors.accent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Resend OTP",
                                style = MaterialTheme.typography.labelLarge,
                                color = CosmicAppTheme.colors.accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    isResending = true
                                    canResend = false
                                    onResendOtp()
                                    isResending = false
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Resend OTP in ${resendTimer}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Help text for users who don't receive OTP
                Text(
                    text = "⚠️ If OTP not received, check SMS spam/blocked folder",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun OtpDigitBox(digit: String, isFocused: Boolean = false) {
    Surface(
        modifier = Modifier
            .size(64.dp),
        shape = RoundedCornerShape(AstroDimens.RadiusMedium),
        color = CosmicAppTheme.colors.bgStart,
        border = BorderStroke(
            width = 2.dp,
            color = if (isFocused) CosmicAppTheme.colors.accent else CosmicAppTheme.colors.cardStroke.copy(alpha = 0.3f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.displaySmall,
                color = CosmicAppTheme.colors.textPrimary
            )
        }
    }
}
