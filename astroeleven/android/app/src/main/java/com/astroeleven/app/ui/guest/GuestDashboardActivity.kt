package com.astroeleven.app.ui.guest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.astroeleven.app.data.model.Astrologer
import com.astroeleven.app.ui.auth.LoginActivity
import com.astroeleven.app.ui.home.ComposeRasiItem
import com.astroeleven.app.ui.home.HomeScreen
import com.astroeleven.app.ui.theme.CosmicAppTheme
import com.astroeleven.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState
import com.astroeleven.app.data.model.Banner
import com.astroeleven.app.data.model.GridService
import com.astroeleven.app.data.api.ApiClient

class GuestDashboardActivity : AppCompatActivity() {

    private val _horoscope = MutableStateFlow<String>("Loading Horoscope...")
    private val _astrologers = MutableStateFlow<List<Astrologer>>(emptyList())
    private val _isLoading = MutableStateFlow<Boolean>(true)
    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    private val _services = MutableStateFlow<List<GridService>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Legacy ThemeManager removed

        setContent {
            CosmicAppTheme {
                val horoscope by _horoscope.collectAsState()
                val astrologers by _astrologers.collectAsState()
                val isLoading by _isLoading.collectAsState()
                val banners by _banners.collectAsState()
                val services by _services.collectAsState()

                var selectedRasiItem by remember { mutableStateOf<ComposeRasiItem?>(null) }

                if (selectedRasiItem != null) {
                   com.astroeleven.app.ui.dashboard.RasiDetailDialog(
                        name = selectedRasiItem!!.name,
                        iconRes = selectedRasiItem!!.iconRes,
                        onDismiss = { selectedRasiItem = null }
                    )
                }

                HomeScreen(
                    walletBalance = 0.0, // Guest has 0 balance
                    horoscope = horoscope,
                    astrologers = astrologers,
                    isLoading = isLoading,
                    banners = banners,
                    services = services,
                    onBannerClick = { _ -> redirectToLogin() },
                    onChatClick = { redirectToLogin() },
                    onCallClick = { _, _ -> redirectToLogin() },
                    onRasiClick = { item -> selectedRasiItem = item },
                    onLogoutClick = { redirectToLogin() }, // Acts as Login button
                    onDrawerItemClick = { item ->
                         if (item == "Login" || item == "Logout") redirectToLogin()
                         else redirectToLogin() // Guest redirects to login for everything ideally
                    },
                    onServiceClick = { handleServiceClick(it) },
                    onWalletClick = { redirectToLogin() },
                    isGuest = true
                )
            }
        }

        loadDailyHoroscope()
        loadAstrologers()
        fetchHomeData()
        setupSocket()
    }

    private fun setupSocket() {
        com.astroeleven.app.data.remote.SocketManager.init()
        val socket = com.astroeleven.app.data.remote.SocketManager.getSocket()
        socket?.connect()

        socket?.on("astro-list") { args ->
            val data = args[0] as? JSONObject ?: return@on
            val arr = data.optJSONArray("list")
            if (arr != null) {
                updateAstrologerList(arr)
            }
        }

        socket?.on("astrologer-update") { args ->
            // Server broadcasts full list on update
            val data = args[0] as org.json.JSONArray
            updateAstrologerList(data)
        }

        socket?.emit("get-astrologers")
    }

    private fun updateAstrologerList(jsonArray: org.json.JSONArray) {
        val list = mutableListOf<Astrologer>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(parseAstrologer(obj))
        }
        val sortedList = list.sortedWith(
            compareByDescending<Astrologer> {
                it.isOnline || it.isChatOnline || it.isAudioOnline || it.isVideoOnline
            }.thenByDescending { it.experience }
        )
        lifecycleScope.launch(Dispatchers.Main) {
            _astrologers.value = sortedList
            _isLoading.value = false
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun loadDailyHoroscope() {
        lifecycleScope.launch {
            try {
                _horoscope.value = fetchHoroscope()
            } catch (e: Exception) {
                Log.e("GuestDashboard", "Error loading horoscope", e)
                _horoscope.value = "Good progress will occur today as Chandrashtama has passed."
            }
        }
    }

    private fun loadAstrologers() {
        _isLoading.value = true
        lifecycleScope.launch {
            try {
                _astrologers.value = fetchAstrologers()
            } catch (e: Exception) {
                Log.e("GuestDashboard", "Error loading astrologers", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchHoroscope(): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("${Constants.SERVER_URL}/api/daily-horoscope")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                json.optString("content", "Today is a good day!")
            } else {
                "Today is a good day!"
            }
        }
    }

    private suspend fun fetchAstrologers(): List<Astrologer> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()
        val result = mutableListOf<Astrologer>()

        try {
            val request = Request.Builder()
                .url("${Constants.SERVER_URL}/api/astrology/astrologers")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val arr = json.optJSONArray("astrologers")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            result.add(parseAstrologer(obj))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result.sortWith(
            compareByDescending<Astrologer> {
                it.isOnline || it.isChatOnline || it.isAudioOnline || it.isVideoOnline
            }.thenByDescending { it.experience }
        )
        result
    }

    // Helper to parse consistent with HomeActivity/ClientDashboard
    private fun parseAstrologer(json: JSONObject): Astrologer {
         val skillsArr = json.optJSONArray("skills")
         val skills = mutableListOf<String>()
         if (skillsArr != null) {
             for (i in 0 until skillsArr.length()) {
                 skills.add(skillsArr.getString(i))
             }
         }

         // Map "charges" to "price" if needed, assuming API structure is consistent
         // Guest logic used "charges", Home logic uses "price".
         // I'll check if "price" exists, fallback to "charges"
         val price = if (json.has("price")) json.getInt("price") else json.optInt("charges", 15)

         return Astrologer(
             userId = json.optString("userId", ""),
             name = json.optString("name", "Astrologer"),
             phone = json.optString("phone", ""),
             skills = skills,
             price = price,
             isOnline = json.optBoolean("isOnline", false),
             isChatOnline = json.optBoolean("isChatOnline", false),
             isAudioOnline = json.optBoolean("isAudioOnline", false),
             isVideoOnline = json.optBoolean("isVideoOnline", false),
             image = json.optString("image", ""),
             experience = json.optInt("experience", 0),
             isVerified = json.optBoolean("isVerified", false),
             walletBalance = json.optDouble("walletBalance", 0.0)
         )
    }

    private fun handleServiceClick(serviceName: String) {
        when (serviceName.replace("\n", " ")) {
            "Free  horoscope" -> {
                val intent = Intent(this, com.astroeleven.app.ui.horoscope.FreeHoroscopeActivity::class.java)
                startActivity(intent)
            }
            "Horoscope Match" -> {
                redirectToLogin()
            }
            "Daily Horoscope" -> {
                val intent = Intent(this, com.astroeleven.app.ui.rasipalan.RasipalanActivity::class.java)
                startActivity(intent)
            }
            "Astro Academy" -> {
                Toast.makeText(this, "Astro Academy - Basic content available!", Toast.LENGTH_SHORT).show()
            }
            "Free  Star Services" -> {
                Toast.makeText(this, "Free Star Services - Available for guests!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "$serviceName clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchHomeData()
    }

    private fun fetchHomeData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.api.getHomeData()
                if (response.isSuccessful && response.body()?.ok == true) {
                    val homeData = response.body()?.data
                    val banners = homeData?.banners ?: emptyList()
                    val services = homeData?.homeConfig?.gridServices ?: emptyList()
                    
                    Log.d("GuestDashboard", "Home data fetched: banners=${banners.size}, services=${services.size}")
                    
                    _banners.value = banners
                    _services.value = services
                } else {
                    Log.e("GuestDashboard", "Home data fetch failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("GuestDashboard", "Error fetching home data: ${e.message}")
            }
        }
    }
}
