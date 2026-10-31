package com.astroeleven.app.ui.chart

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.astroeleven.app.ui.theme.CosmicAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

class ChartDisplayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val birthDataStr = intent.getStringExtra("birthData")
        var birthData: JSONObject? = null

        if (birthDataStr != null) {
            try {
                birthData = JSONObject(birthDataStr)
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid Birth Data", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } else {
            Toast.makeText(this, "No Birth Data Provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            CosmicAppTheme {
                ChartDisplayScreen(
                    birthData = birthData!!,
                    onFetchChart = { bData -> fetchChartHtml(bData) }
                )
            }
        }
    }

    private suspend fun fetchChartHtml(birthData: JSONObject): String? = withContext(Dispatchers.IO) {
        try {
            val apiInterface = com.astroeleven.app.data.api.ApiClient.api
            val dateStr = String.format("%04d-%02d-%02d", birthData.optInt("year"), birthData.optInt("month"), birthData.optInt("day"))
            val timeStr = String.format("%02d:%02d", birthData.optInt("hour"), birthData.optInt("minute"))

            val payload = com.google.gson.JsonObject().apply {
                addProperty("date", dateStr)
                addProperty("time", timeStr)
                addProperty("lat", birthData.optDouble("latitude"))
                addProperty("lng", birthData.optDouble("longitude"))
                addProperty("timezone", birthData.optDouble("timezone", 5.5))
                addProperty("name", birthData.optString("name"))
                addProperty("city", birthData.optString("city", birthData.optString("district", "")))
            }

            val response = apiInterface.getRasiEngBirthChart(payload)
            if (response.isSuccessful && response.body() != null) {
                val jsonResponse = JSONObject(response.body().toString())
                if (jsonResponse.has("data")) {
                    val data = jsonResponse.getJSONObject("data")
                    generateHtml(data, birthData)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Tamil name map for planets
    private val tamilPlanetNames = mapOf(
        "Sun" to "சூ", "Moon" to "சந்", "Mars" to "செ",
        "Mercury" to "பு", "Jupiter" to "கு", "Venus" to "சு",
        "Saturn" to "ச", "Rahu" to "ரா", "Ketu" to "கே",
        "Ascendant" to "ல", "Mandi" to "மா"
    )

    // Tamil sign names
    private val tamilSignNames = mapOf(
        "Aries" to "மேஷம்", "Taurus" to "ரிஷபம்", "Gemini" to "மிதுனம்",
        "Cancer" to "கடகம்", "Leo" to "சிம்மம்", "Virgo" to "கன்னி",
        "Libra" to "துலாம்", "Scorpio" to "விருச்சிகம்", "Sagittarius" to "தனுசு",
        "Capricorn" to "மகரம்", "Aquarius" to "கும்பம்", "Pisces" to "மீனம்"
    )

    private fun generateHtml(data: JSONObject, inputData: JSONObject): String {
        // API returns planets as JSONArray, not JSONObject
        val planetsArray = data.getJSONArray("planets")
        val panchangam = data.optJSONObject("panchanga") ?: JSONObject()
        val dasha = data.optJSONArray("dasha")
        val navamsaObj = data.optJSONObject("navamsa")
        val navamsaPlanets = navamsaObj?.optJSONArray("planets") ?: JSONArray()

        // South Indian chart box mapping (12 boxes)
        val signMap = mapOf(
            "Pisces" to 0, "Aries" to 1, "Taurus" to 2, "Gemini" to 3,
            "Aquarius" to 4, "Cancer" to 5, "Capricorn" to 6, "Leo" to 7,
            "Sagittarius" to 8, "Scorpio" to 9, "Libra" to 10, "Virgo" to 11
        )

        val rasiBoxes = Array(12) { StringBuilder() }

        // Process planets from array
        for (i in 0 until planetsArray.length()) {
            val planet = planetsArray.getJSONObject(i)
            val name = planet.optString("name", "")
            val sign = planet.optString("signName", planet.optString("sign", ""))
            val tamilName = tamilPlanetNames[name] ?: name.take(2)
            val cssClass = if (name == "Ascendant") "planet lagna" else "planet"

            signMap[sign]?.let { idx ->
                rasiBoxes[idx].append("<div class='$cssClass'>$tamilName</div>")
            }
        }

        // Navamsa chart
        val navamsaBoxes = Array(12) { StringBuilder() }
        for (i in 0 until navamsaPlanets.length()) {
            val planet = navamsaPlanets.getJSONObject(i)
            val name = planet.optString("name", "")
            val sign = planet.optString("signName", "")
            val tamilName = tamilPlanetNames[name] ?: name.take(2)

            signMap[sign]?.let { idx ->
                navamsaBoxes[idx].append("<div class='planet'>$tamilName</div>")
            }
        }

        // Build planet details table from array
        val planetTableHtml = buildString {
            val displayOrder = listOf("Ascendant", "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu", "Mandi")
            for (pName in displayOrder) {
                for (i in 0 until planetsArray.length()) {
                    val p = planetsArray.getJSONObject(i)
                    if (p.optString("name") == pName) {
                        val tName = if (pName == "Ascendant") "லக்னம்" else if (pName == "Mandi") "மாந்தி" else (tamilPlanetNames[pName] ?: pName)
                        val sign = tamilSignNames[p.optString("signName", p.optString("sign", ""))] ?: p.optString("sign", "")
                        val deg = p.optString("degreeFormatted", "")
                        val naks = p.optString("nakshatraName", p.optString("nakshatra", ""))
                        val pada = p.optInt("nakshatraPada", 1)
                        append("<tr><td>$tName</td><td>$sign</td><td>$deg</td><td>$naks</td><td>$pada</td></tr>")
                        break
                    }
                }
            }
        }

        val clientInfo = data.optJSONObject("clientInfo")
        val clientName = clientInfo?.optString("name")?.takeIf { it.isNotBlank() } ?: inputData.optString("name", "User")
        val clientDob = clientInfo?.optString("dob")?.takeIf { it.isNotBlank() } ?: "${inputData.optInt("day")}/${inputData.optInt("month")}/${inputData.optInt("year")}"
        val clientTob = clientInfo?.optString("tob")?.takeIf { it.isNotBlank() } ?: "${inputData.optInt("hour")}:${inputData.optInt("minute")}"
        val clientPob = clientInfo?.optString("pob")?.takeIf { it.isNotBlank() } ?: inputData.optString("city", inputData.optString("district", "-"))

        val currentDashaInfo = data.optJSONObject("currentDashaInfo")
        val lagnaRoles = data.optJSONObject("lagnaRoles")

        var moonSign = ""
        var ascSign = ""
        var moonNakshatra = ""
        for (i in 0 until planetsArray.length()) {
            val p = planetsArray.getJSONObject(i)
            val pName = p.optString("name")
            if (pName == "Moon") {
                moonSign = tamilSignNames[p.optString("signName", p.optString("sign", ""))] ?: p.optString("sign", "")
                moonNakshatra = p.optString("nakshatraName", p.optString("nakshatra", ""))
            } else if (pName == "Ascendant") {
                ascSign = tamilSignNames[p.optString("signName", p.optString("sign", ""))] ?: p.optString("sign", "")
            }
        }

        // Dasha info from array or currentDashaInfo
        val dashaCardHtml = if (currentDashaInfo != null && (currentDashaInfo.optString("dasaLord").isNotBlank() || currentDashaInfo.optString("dashaLord").isNotBlank() || currentDashaInfo.optString("dasaDisplayText").isNotBlank() || currentDashaInfo.optString("dashaDisplay").isNotBlank())) {
            val dLord = currentDashaInfo.optString("dasaLordTamil").ifEmpty { currentDashaInfo.optString("dashaLordTamil").ifEmpty { currentDashaInfo.optString("dasaLord").ifEmpty { currentDashaInfo.optString("dashaLord") } } }
            val dStar = currentDashaInfo.optString("dasaStarLordTamil").ifEmpty { currentDashaInfo.optString("dashaStarLordTamil").ifEmpty { currentDashaInfo.optString("dasaStarLord").ifEmpty { currentDashaInfo.optString("dashaStarLord") } } }
            val dStart = currentDashaInfo.optString("dasaStartDate").ifEmpty { currentDashaInfo.optString("dashaStartDate") }
            val dEnd = currentDashaInfo.optString("dasaEndDate").ifEmpty { currentDashaInfo.optString("dashaEndDate") }

            val bLord = currentDashaInfo.optString("bhuktiLordTamil").ifEmpty { currentDashaInfo.optString("bhuktiLord") }
            val bStar = currentDashaInfo.optString("bhuktiStarLordTamil").ifEmpty { currentDashaInfo.optString("bhuktiStarLord") }
            val bStart = currentDashaInfo.optString("bhuktiStartDate")
            val bEnd = currentDashaInfo.optString("bhuktiEndDate")

            """
            <div class="card" style="background:#FFF9E6; border:1px solid #FFE082;">
                <h4 style="margin:0 0 8px 0; color:#B78103;">நடப்பு திசா புத்தி</h4>
                <div style="font-weight:bold; color:#4E342E; font-size:13px;">$dLord திசை ($dStart To $dEnd)</div>
                <div style="font-size:12px; color:#6D4C41; margin-bottom:6px;">திசாநாதன் நின்ற நட்சத்திர அதிபதி: $dStar</div>
                <div style="font-weight:bold; color:#4E342E; font-size:13px;">$bLord புத்தி ($bStart To $bEnd)</div>
                <div style="font-size:12px; color:#6D4C41;">புத்திநாதன் நின்ற நட்சத்திர அதிபதி: $bStar</div>
            </div>
            """
        } else ""

        val lagnaRolesCardHtml = if (lagnaRoles != null) {
            val subhar = lagnaRoles.optString("subhar", "-")
            val paavar = lagnaRoles.optString("paavar", "-")
            val badhaka = lagnaRoles.optString("badhakathipathi").ifEmpty { lagnaRoles.optString("badhaka", "-") }
            val maraka = lagnaRoles.optString("marakathipathi").ifEmpty { lagnaRoles.optString("maraka", "-") }
            val lName = lagnaRoles.optString("lagnaTamil").ifEmpty { ascSign }

            """
            <div class="card" style="background:#F9FBE7; border:1px solid #DCEDC8;">
                <h4 style="margin:0 0 8px 0; color:#33691E;">லக்ன கிரக நிலைகள் ($lName லக்னம்)</h4>
                <div style="display:flex; justify-content:space-between; margin-bottom:6px;">
                    <div><span style="font-weight:bold; color:#2E7D32;">லக்ன சுபர்:</span> $subhar</div>
                    <div><span style="font-weight:bold; color:#C62828;">லக்ன பாவர்:</span> $paavar</div>
                </div>
                <div style="display:flex; justify-content:space-between;">
                    <div><span style="font-weight:bold; color:#E65100;">பாதகாதிபதி:</span> $badhaka</div>
                    <div><span style="font-weight:bold; color:#4A148C;">மாறகாதிபதி:</span> $maraka</div>
                </div>
            </div>
            """
        } else ""

        return """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 16px; background: #FCFCFC; color: #1A1A1A; }
                    h2, h3 { text-align: center; color: #E1353C; margin: 24px 0 12px 0; border-bottom: 1px solid #E2E8F0; padding-bottom: 8px; font-weight: 600; letter-spacing: 1px; }
                    .card { background:#FFFFFF; border-radius:10px; padding:14px; margin-bottom:14px; box-shadow:0 2px 6px rgba(0,0,0,0.04); }
                    .chart-container {
                        display: grid;
                        grid-template-columns: 1fr 1fr 1fr 1fr;
                        grid-template-rows: 1fr 1fr 1fr 1fr;
                        gap: 1px;
                        background: #E1353C;
                        border: 3px solid #E1353C;
                        width: 100%;
                        aspect-ratio: 1 / 1;
                        margin-bottom: 30px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.06);
                        border-radius: 6px;
                        overflow: hidden;
                    }
                    .box { background: #FFFFFF; padding: 4px; font-size: 11px; display: flex; flex-wrap: wrap; align-content: center; justify-content: center; min-height: 40px; border: 0.5px solid #E2E8F0; }
                    .b0 { grid-column: 1; grid-row: 1; }
                    .b1 { grid-column: 2; grid-row: 1; }
                    .b2 { grid-column: 3; grid-row: 1; }
                    .b3 { grid-column: 4; grid-row: 1; }
                    .b4 { grid-column: 1; grid-row: 2; }
                    .center-box { grid-column: 2 / span 2; grid-row: 2 / span 2; background: #FFFFFF; display: flex; align-items: center; justify-content: center; font-weight: bold; color: #E1353C; font-size: 20px; text-transform: uppercase; letter-spacing: 2px; }
                    .b5 { grid-column: 4; grid-row: 2; }
                    .b6 { grid-column: 1; grid-row: 3; }
                    .b7 { grid-column: 4; grid-row: 3; }
                    .b8 { grid-column: 1; grid-row: 4; }
                    .b9 { grid-column: 2; grid-row: 4; }
                    .b10 { grid-column: 3; grid-row: 4; }
                    .b11 { grid-column: 4; grid-row: 4; }
                    .planet { background: #FFFDE7; padding: 2px 6px; margin: 2px; border-radius: 4px; color: #B71C1C; font-weight: bold; border: 1px solid #FDBA16; font-size: 10px; }
                    .planet.lagna { background: #E1353C; color: #FFFFFF; border-color: #E1353C; }
                    .info-table { width: 100%; border-collapse: collapse; margin-top: 20px; border: 1px solid #E2E8F0; background: #FFFFFF; border-radius: 12px; overflow: hidden; }
                    .info-table td, .info-table th { border: 1px solid #E2E8F0; padding: 12px; text-align: left; font-size: 13px; }
                    .info-table th { background-color: #F4F4F4; color: #1A1A1A; font-weight: 600; }
                    .highlight { background-color: #FFEBEE; font-weight: bold; border: 2px solid #E1353C; color: #E1353C; }
                </style>
            </head>
            <body>
                <!-- 1. Client Details Header -->
                <div class="card" style="background:#FFF8F0; border:1px solid #FFD180;">
                    <h4 style="margin:0 0 10px 0; color:#8B2500;">ஜாதகர் விவரங்கள்</h4>
                    <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; font-size:13px;">
                        <div><span style="color:#777;">பெயர்:</span> <strong>$clientName</strong></div>
                        <div><span style="color:#777;">பிறந்த தேதி:</span> <strong>$clientDob</strong></div>
                        <div><span style="color:#777;">பிறந்த நேரம்:</span> <strong>$clientTob</strong></div>
                        <div><span style="color:#777;">பிறந்த மாவட்டம்:</span> <strong>$clientPob</strong></div>
                    </div>
                </div>

                <!-- 2. Rasi, Lagna, Nakshatra -->
                <div class="card" style="background:#F3F7FF; border:1px solid #BBDEFB; display:flex; justify-content:space-around; text-align:center;">
                    <div>
                        <div style="font-size:11px; color:#1565C0;">லக்னம்</div>
                        <div style="font-size:15px; font-weight:bold; color:#0D47A1;">$ascSign</div>
                    </div>
                    <div style="border-left:1px solid #BBDEFB; height:28px;"></div>
                    <div>
                        <div style="font-size:11px; color:#1565C0;">ராசி</div>
                        <div style="font-size:15px; font-weight:bold; color:#0D47A1;">$moonSign</div>
                    </div>
                    <div style="border-left:1px solid #BBDEFB; height:28px;"></div>
                    <div>
                        <div style="font-size:11px; color:#1565C0;">நட்சத்திரம்</div>
                        <div style="font-size:14px; font-weight:bold; color:#0D47A1;">$moonNakshatra</div>
                    </div>
                </div>

                $dashaCardHtml
                $lagnaRolesCardHtml

                <h3>Rasi Chart</h3>
                <div class="chart-container">
                    <div class="box b0">${rasiBoxes[0]}</div>
                    <div class="box b1">${rasiBoxes[1]}</div>
                    <div class="box b2">${rasiBoxes[2]}</div>
                    <div class="box b3">${rasiBoxes[3]}</div>
                    <div class="box b4">${rasiBoxes[4]}</div>
                    <div class="center-box">RASI</div>
                    <div class="box b5">${rasiBoxes[5]}</div>
                    <div class="box b6">${rasiBoxes[6]}</div>
                    <div class="box b7">${rasiBoxes[7]}</div>
                    <div class="box b8">${rasiBoxes[8]}</div>
                    <div class="box b9">${rasiBoxes[9]}</div>
                    <div class="box b10">${rasiBoxes[10]}</div>
                    <div class="box b11">${rasiBoxes[11]}</div>
                </div>

                <h3>Navamsa Chart</h3>
                <div class="chart-container">
                    <div class="box b0">${navamsaBoxes[0]}</div>
                    <div class="box b1">${navamsaBoxes[1]}</div>
                    <div class="box b2">${navamsaBoxes[2]}</div>
                    <div class="box b3">${navamsaBoxes[3]}</div>
                    <div class="box b4">${navamsaBoxes[4]}</div>
                    <div class="center-box">NAVAMSA</div>
                    <div class="box b5">${navamsaBoxes[5]}</div>
                    <div class="box b6">${navamsaBoxes[6]}</div>
                    <div class="box b7">${navamsaBoxes[7]}</div>
                    <div class="box b8">${navamsaBoxes[8]}</div>
                    <div class="box b9">${navamsaBoxes[9]}</div>
                    <div class="box b10">${navamsaBoxes[10]}</div>
                    <div class="box b11">${navamsaBoxes[11]}</div>
                </div>

                <h3>Panchangam</h3>
                <table class="info-table">
                    <tr><th>Tithi</th><td>${panchangam.optString("tithi")}</td></tr>
                    <tr><th>Nakshatra</th><td>${panchangam.optString("nakshatra")}</td></tr>
                    <tr><th>Yoga</th><td>${panchangam.optString("yoga")}</td></tr>
                    <tr><th>Karana</th><td>${panchangam.optString("karana")}</td></tr>
                </table>

                <h3>நவகிரக பாதசாரம் (Navagraha Pathasaram)</h3>
                <table class="info-table">
                    <thead>
                        <tr>
                            <th>கிரகம்</th>
                            <th>ராசி</th>
                            <th>பாகை</th>
                            <th>நட்சத்திரம்</th>
                            <th>பாதம்</th>
                        </tr>
                    </thead>
                    <tbody>
                        $planetTableHtml
                    </tbody>
                </table>

            </body>
            </html>
        """
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDisplayScreen(
    birthData: JSONObject,
    onFetchChart: suspend (JSONObject) -> String?
) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = onFetchChart(birthData)
        if (result != null) {
            htmlContent = result
        } else {
            failed = true
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Birth Chart Analysis") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { /* finish handled in Activity or via back handler */ }) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CosmicAppTheme.colors.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicAppTheme.colors.bgStart,
                    titleContentColor = CosmicAppTheme.colors.textPrimary
                )
            )
        },
        containerColor = CosmicAppTheme.colors.bgStart
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CosmicAppTheme.colors.accent
                )
            } else if (failed) {
                 Text(
                     text = "Failed to load chart data.",
                     color = Color.Red,
                     modifier = Modifier.align(Alignment.Center)
                 )
            } else if (htmlContent != null) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, htmlContent!!, "text/html", "utf-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
