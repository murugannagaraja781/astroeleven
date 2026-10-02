package com.astroeleven.app.ui.payment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.astroeleven.app.data.api.ApiClient
import com.astroeleven.app.data.local.TokenManager
import com.astroeleven.app.data.model.PaymentInitiateRequest
import kotlinx.coroutines.launch

/**
 * PaymentActivity - Handles PhonePe V2 PG Checkout Gateway inside an INLINE App WebView.
 */
class PaymentActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PaymentActivity"
    }

    private lateinit var tokenManager: TokenManager
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Programmatic UI
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#150E0C"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

        statusText = TextView(this).apply {
            text = "Connecting to PhonePe..."
            textSize = 18f
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            visibility = android.view.View.GONE
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                javaScriptCanOpenWindowsAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportMultipleWindows(false)
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true

                // Clean Mobile Chrome User-Agent
                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            }

            android.webkit.CookieManager.getInstance().let { cm ->
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(this, true)
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress >= 90) {
                        progressBar.visibility = android.view.View.GONE
                        statusText.visibility = android.view.View.GONE
                        webView.visibility = android.view.View.VISIBLE
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url != null) {
                        Log.d(TAG, "Inline WebView Loading URL (legacy): $url")
                        if (url.startsWith("astroeleven://payment-success") || url.contains("payment-success")) {
                            handlePaymentResult("success")
                            return true
                        }
                        if (url.startsWith("astroeleven://payment-failed") || url.contains("payment-failed")) {
                            handlePaymentResult("failed")
                            return true
                        }
                        return handleExternalIntents(url)
                    }
                    return false
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url.toString()
                    Log.d(TAG, "Inline WebView Loading URL: $url")

                    if (url.startsWith("astroeleven://payment-success") || url.contains("payment-success")) {
                        handlePaymentResult("success")
                        return true
                    }
                    if (url.startsWith("astroeleven://payment-failed") || url.contains("payment-failed")) {
                        handlePaymentResult("failed")
                        return true
                    }

                    return handleExternalIntents(url)
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                    handler?.proceed()
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    Log.e(TAG, "WebView Error: ${error?.description}")
                }
            }
        }

        layout.addView(progressBar)
        layout.addView(statusText)
        layout.addView(webView)
        setContentView(layout)

        tokenManager = TokenManager(this)

        if (intent != null && intent.data != null) {
            handleDeepLink(intent)
            return
        }

        val amount = intent.getDoubleExtra("amount", 0.0)
        if (amount <= 0.0) {
            showError("Invalid Amount: $amount")
            return
        }

        startPhonePeCheckout(amount)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            val url = data.toString()
            Log.d(TAG, "Received DeepLink: $url")
            if (url.startsWith("astroeleven://payment-success") || url.contains("payment-success")) {
                handlePaymentResult("success")
            } else if (url.startsWith("astroeleven://payment-failed") || url.contains("payment-failed")) {
                handlePaymentResult("failed")
            }
        }
    }

    private fun startPhonePeCheckout(amount: Double) {
        val userId = tokenManager.getUserSession()?.userId
        if (userId.isNullOrEmpty()) {
            showError("User session expired. Please log in again.")
            return
        }

        val isSuperWallet = intent.getBooleanExtra("isSuperWallet", false)
        val offerPercentage = intent.getDoubleExtra("offerPercentage", 0.0)
        val promoCode = intent.getStringExtra("promoCode")

        lifecycleScope.launch {
            try {
                statusText.text = "Initializing PhonePe Payment..."
                val request = PaymentInitiateRequest(
                    userId = userId,
                    amount = amount.toInt(),
                    isApp = true,
                    promoCode = promoCode,
                    isSuperWallet = isSuperWallet,
                    offerPercentage = offerPercentage
                )

                // Step 1: Generate Payment Token
                val tokenRes = ApiClient.api.getPaymentToken(request)
                if (tokenRes.isSuccessful && tokenRes.body()?.get("ok")?.asBoolean == true) {
                    val token = tokenRes.body()?.get("token")?.asString
                    if (!token.isNullOrEmpty()) {
                        val paymentHtmlUrl = "https://astroeleven.com/payment.html?token=$token&isApp=true"
                        runOnUiThread {
                            val headers = HashMap<String, String>()
                            headers["Referer"] = "https://astroeleven.com/"
                            webView.loadUrl(paymentHtmlUrl, headers)
                        }
                        return@launch
                    }
                }

                // Fallback: Direct PhonePe Payment URL with Referer Header
                val res = ApiClient.api.initiatePayment(request)
                if (res.isSuccessful && res.body()?.ok == true) {
                    val url = res.body()?.paymentUrl
                    if (!url.isNullOrEmpty()) {
                        runOnUiThread {
                            val headers = HashMap<String, String>()
                            headers["Referer"] = "https://astroeleven.com/"
                            webView.loadUrl(url, headers)
                        }
                    } else {
                        showError("Failed to fetch payment checkout link.")
                    }
                } else {
                    val errMsg = res.body()?.error ?: "Payment initiation failed (${res.code()})"
                    showError(errMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Payment initiation exception", e)
                showError(e.localizedMessage ?: "Network error initiating payment")
            }
        }
    }

    private fun handleExternalIntents(url: String): Boolean {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false // Keep web pages inside Inline WebView!
        }

        try {
            val intent = if (url.startsWith("intent://")) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val pm = packageManager
                if (intent.resolveActivity(pm) != null) {
                    startActivity(intent)
                    return true
                } else {
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (!fallbackUrl.isNullOrEmpty()) {
                        webView.loadUrl(fallbackUrl)
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling external intent: $url", e)
        }
        return true
    }

    private fun showError(message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                AlertDialog.Builder(this)
                    .setTitle("Payment Message")
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .show()
            }
        }
    }

    private fun handlePaymentResult(status: String) {
        runOnUiThread {
            if (status == "success") {
                Toast.makeText(this, "Payment Completed Successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Payment Cancelled or Failed.", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }
}
