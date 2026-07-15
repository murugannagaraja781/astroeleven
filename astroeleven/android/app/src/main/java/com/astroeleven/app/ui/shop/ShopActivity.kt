package com.astroeleven.app.ui.shop

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.astroeleven.app.data.local.TokenManager
import com.astroeleven.app.ui.theme.CosmicAppTheme
import com.astroeleven.app.utils.Constants

class ShopActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)

        val userSession = tokenManager.getUserSession()
        val referralCode = userSession?.referralCode ?: ""

        val serverUrl = Constants.SERVER_URL
        val shopUrl = if (referralCode.isNotEmpty()) {
            "$serverUrl/shop.html?code=$referralCode"
        } else {
            "$serverUrl/shop.html"
        }

        setContent {
            CosmicAppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Astro Eleven Store", style = MaterialTheme.typography.titleMedium) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFFFFC700), // Sunlight Yellow
                                titleContentColor = Color(0xFFB3262A), // Astro Crimson
                                navigationIconContentColor = Color(0xFFB3262A)
                            )
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(Color.White)
                    ) {
                        ShopWebView(shopUrl)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ShopWebView(url: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
