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
            val displayOrder = listOf("Ascendant", "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu")
            for (pName in displayOrder) {
                for (i in 0 until planetsArray.length()) {
                    val p = planetsArray.getJSONObject(i)
                    if (p.optString("name") == pName) {
                        val tName = if (pName == "Ascendant") "லக்னம்" else (tamilPlanetNames[pName] ?: pName)
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

        // Dasha info from array
        val dashaHtml = if (dasha != null && dasha.length() > 0) {
            val currentDasha = dasha.getJSONObject(0)
            val lord = currentDasha.optString("lord", "")
            val endsAt = currentDasha.optString("end", "").take(10)
            val subPeriods = currentDasha.optJSONArray("subPeriods")
            val bhukti = if (subPeriods != null && subPeriods.length() > 0) subPeriods.getJSONObject(0).optString("lord", "") else ""

            """
            <h3>Current Dasha</h3>
            <table class="info-table highlight">
                <tr><th>Lord</th><td>$lord</td></tr>
                <tr><th>Bhukti</th><td>$bhukti</td></tr>
                <tr><th>Ends At</th><td>$endsAt</td></tr>
            </table>
            """
        } else ""

        return """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 16px; background: #FCFCFC; color: #1A1A1A; }
                    h2, h3 { text-align: center; color: #E1353C; margin: 24px 0 12px 0; border-bottom: 1px solid #E2E8F0; padding-bottom: 8px; font-weight: 600; letter-spacing: 1px; }
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
                <h3>${inputData.optString("name")}'s Chart</h3>
                <p style="text-align:center; font-size:12px;">${inputData.optString("city")} | ${inputData.optInt("day")}-${inputData.optInt("month")}-${inputData.optInt("year")}</p>

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
                $dashaHtml

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
