package com.astroeleven.app.ui.chart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.astroeleven.app.R
import com.astroeleven.app.ui.theme.CosmicAppTheme
import com.google.gson.Gson
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// --- Aesthetic Constants (Premium Light Theme) ---
val ParchmentBase = Color(0xFFFCF6F0) // Light Cream
val ParchmentLight = Color(0xFFFFFFFF) // White
val ChocolateBrown = Color(0xFFE1353C) // Brand Red
val BorderColor = Color(0xFFE0D5C9)
val ChartLineColor = Color(0xFFD4B99F).copy(alpha = 0.8f)

// --- Tamil Translation Constants ---
val signTamil = mapOf(
    "Aries" to "மேஷம்", "Taurus" to "ரிஷபம்", "Gemini" to "மிதுனம்", "Cancer" to "கடகம்",
    "Leo" to "சிம்மம்", "Virgo" to "கன்னி", "Libra" to "துலாம்", "Scorpio" to "விருச்சிகம்",
    "Sagittarius" to "தனுசு", "Capricorn" to "மகரம்", "Aquarius" to "கும்பம்", "Pisces" to "மீனம்"
)

val planetTamil = mapOf(
    "Sun" to "சூரியன்", "Moon" to "சந்திரன்", "Mars" to "செவ்வாய்", "Mercury" to "புதன்",
    "Jupiter" to "குரு", "Venus" to "சுக்கிரன்", "Saturn" to "சனி", "Rahu" to "ராகு",
    "Ketu" to "கேது", "Ascendant" to "லக்னம்"
)

val nakshatraTamil = mapOf(
    "Ashwini" to "அஸ்வினி", "Bharani" to "பரணி", "Krittika" to "கார்த்திகை", "Rohini" to "ரோகிணி", "Mrigashira" to "மிருகசீரிடம்",
    "Ardra" to "திருவாதிரை", "Punarvasu" to "புனர்பூசம்", "Pushya" to "பூசம்", "Ashlesha" to "ஆயிலியம்", "Magha" to "மகம்",
    "Purva Phalguni" to "பூரம்", "Uttara Phalguni" to "உத்திரம்", "Hasta" to "அஸ்தம்", "Chitra" to "சித்திரை", "Swati" to "சுவாதி",
    "Vishakha" to "விசாகம்", "Anuradha" to "அனுஷம்", "Jyeshtha" to "கேட்டை", "Mula" to "மூலம்", "Purva Ashadha" to "பூராடம்",
    "Uttara Ashadha" to "உத்திராடம்", "Shravana" to "திருவோணம்", "Dhanishta" to "அவிட்டம்", "Shatabhisha" to "சதயம்",
    "Purva Bhadrapada" to "பூரட்டாதி", "Uttara Bhadrapada" to "உத்திரட்டாதி", "Revati" to "ரேவதி"
)

val nakshatraLordTamil = mapOf(
    // Ketu stars
    "Ashwini" to "கேது", "அஸ்வினி" to "கேது",
    "Magha" to "கேது", "மகம்" to "கேது",
    "Mula" to "கேது", "மூலம்" to "கேது",

    // Venus stars
    "Bharani" to "சுக்கிரன்", "பரணி" to "சுக்கிரன்",
    "Purva Phalguni" to "சுக்கிரன்", "பூரம்" to "சுக்கிரன்",
    "Purva Ashadha" to "சுக்கிரன்", "பூராடம்" to "சுக்கிரன்",

    // Sun stars
    "Krittika" to "சூரியன்", "கார்த்திகை" to "சூரியன்",
    "Uttara Phalguni" to "சூரியன்", "உத்திரம்" to "சூரியன்",
    "Uttara Ashadha" to "சூரியன்", "உத்திராடம்" to "சூரியன்",

    // Moon stars
    "Rohini" to "சந்திரன்", "ரோகிணி" to "சந்திரன்",
    "Hasta" to "சந்திரன்", "அஸ்தம்" to "சந்திரன்",
    "Shravana" to "சந்திரன்", "திருவோணம்" to "சந்திரன்",

    // Mars stars
    "Mrigashira" to "செவ்வாய்", "மிருகசீரிடம்" to "செவ்வாய்", "மிருகசீரிஷம்" to "செவ்வாய்",
    "Chitra" to "செவ்வாய்", "சித்திரை" to "செவ்வாய்",
    "Dhanishta" to "செவ்வாய்", "அவிட்டம்" to "செவ்வாய்",

    // Rahu stars
    "Ardra" to "ராகு", "திருவாதிரை" to "ராகு",
    "Swati" to "ராகு", "சுவாதி" to "ராகு",
    "Shatabhisha" to "ராகு", "சதயம்" to "ராகு",

    // Jupiter stars
    "Punarvasu" to "குரு", "புனர்பூசம்" to "குரு",
    "Vishakha" to "குரு", "விசாகம்" to "குரு",
    "Purva Bhadrapada" to "குரு", "பூரட்டாதி" to "குரு",

    // Saturn stars
    "Pushya" to "சனி", "பூசம்" to "சனி",
    "Anuradha" to "சனி", "அனுஷம்" to "சனி",
    "Uttara Bhadrapada" to "சனி", "உத்திரட்டாதி" to "சனி",

    // Mercury stars
    "Ashlesha" to "புதன்", "ஆயில்யம்" to "புதன்", "ஆயிலியம்" to "புதன்",
    "Jyeshtha" to "புதன்", "கேட்டை" to "புதன்",
    "Revati" to "புதன்", "ரேவதி" to "புதன்"
)

val planetAbbrTamil = mapOf(
    "Sun" to "சூரி", "Moon" to "சந்", "Mars" to "செவ்", "Mercury" to "புத",
    "Jupiter" to "குரு", "Venus" to "சுக்", "Saturn" to "சனி", "Rahu" to "ராகு",
    "Ketu" to "கேது", "Ascendant" to "லக்", "As" to "லக்", "Mandi" to "மாந்"
)
 
val dashaLevelTamil = mapOf(
    1 to "மகா தசை",
    2 to "புத்தி",
    3 to "அந்தரம்",
    4 to "பிரத்யந்தரம்",
    5 to "சூட்சமம்"
)

data class RasiClassification(
    val lordEn: String,
    val lordTa: String,
    val subhargal: String,
    val paabargal: String,
    val maaragar: String
)

val rasiClassifications = mapOf(
    "Aries" to RasiClassification("Mars", "செவ்வாய்", "செவ்வாய், சூரியன், குரு", "புதன், சுக்கிரன், சனி", "சுக்கிரன், புதன்"),
    "Taurus" to RasiClassification("Venus", "சுக்கிரன்", "சுக்கிரன், புதன், சனி", "செவ்வாய், சந்திரன், குரு", "செவ்வாய், குரு"),
    "Gemini" to RasiClassification("Mercury", "புதன்", "புதன், சுக்கிரன், சனி", "செவ்வாய், சூரியன், குரு", "சந்திரன், குரு"),
    "Cancer" to RasiClassification("Moon", "சந்திரன்", "சந்திரன், செவ்வாய், குரு", "புதன், சுக்கிரன், சனி", "சனி, புதன்"),
    "Leo" to RasiClassification("Sun", "சூரியன்", "சூரியன், செவ்வாய், குரு", "சுக்கிரன், சனி", "புதன், சனி"),
    "Virgo" to RasiClassification("Mercury", "புதன்", "புதன், சுக்கிரன், சனி", "சூரியன், செவ்வாய், குரு", "செவ்வாய், குரு"),
    "Libra" to RasiClassification("Venus", "சுக்கிரன்", "சுக்கிரன், சனி, புதன்", "செவ்வாய், குரு, சூரியன்", "செவ்வாய், குரு"),
    "Scorpio" to RasiClassification("Mars", "செவ்வாய்", "செவ்வாய், குரு, சூரியன்", "சனி, சுக்கிரன், புதன்", "சுக்கிரன், புதன்"),
    "Sagittarius" to RasiClassification("Jupiter", "குரு", "குரு, செவ்வாய், சூரியன்", "சந்திரன், சுக்கிரன், புதன்", "சனி, புதன்"),
    "Capricorn" to RasiClassification("Saturn", "சனி", "சனி, சுக்கிரன், புதன்", "சூரியன், சந்திரன், செவ்வாய்", "சூரியன், சந்திரன்"),
    "Aquarius" to RasiClassification("Saturn", "சனி", "சனி, புதன், சுக்கிரன்", "சூரியன், சந்திரன், செவ்வாய்", "சூரியன், செவ்வாய்"),
    "Pisces" to RasiClassification("Jupiter", "குரு", "குரு, சந்திரன், செவ்வாய்", "சூரியன், புதன், சுக்கிரன், சனி", "புதன், சுக்கிரன், சனி")
)

// --- Updated Data Models ---
@Keep data class ChartResponse(val success: Boolean = false, val data: ChartData? = null)
@Keep data class ChartData(
    val planets: List<Planet> = emptyList(),
    val houses: HouseData = HouseData(),
    val panchanga: Panchanga = Panchanga(),
    val dasha: List<DashaPeriod> = emptyList(),
    val transits: List<Transit> = emptyList(),
    val tamilDate: TamilDate? = null,
    val kpSignificators: KPSignificators? = null,
    val navamsa: NavamsaData? = null
)

@Keep data class Planet(
    val name: String = "",
    val signName: String = "",
    val signIndex: Int = 0,
    val house: Int = 1,
    val nakshatra: String = "",
    val nakshatraPada: Int = 1,
    val degreeFormatted: String? = null,
    val signLord: String? = null,
    val starLord: String? = null,
    val subLord: String? = null,
    val isRetrograde: Boolean = false,
    val isCombust: Boolean = false
)

@Keep data class HouseData(
    val cusps: List<Double> = emptyList(),
    val details: List<HouseDetail> = emptyList(),
    val ascendantDetails: HouseDetail = HouseDetail()
)

@Keep data class HouseDetail(
    val signName: String = "Aries",
    val signAbbr: String? = null,
    val nakshatra: String? = null,
    val nakshatraPada: Int? = null,
    val signLord: String? = null,
    val starLord: String? = null,
    val subLord: String? = null,
    val degreeFormatted: String? = null
)

@Keep data class NameObject(val name: String? = null)

@Keep data class Panchanga(
    val tithi: NameObject? = null,
    val nakshatra: NameObject? = null,
    val yoga: NameObject? = null,
    val karana: NameObject? = null,
    val vara: NameObject? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val moonSign: String? = null,
    val sunSign: String? = null
)
@Keep data class DashaPeriod(
    val lord: String = "",
    val start: String = "",
    val end: String = "",
    val level: Int = 1,
    val subPeriods: List<DashaPeriod>? = null
)
@Keep data class Transit(val name: String = "", val signName: String = "", val isRetrograde: Boolean = false)
@Keep data class TamilDate(val day: Int = 1, val month: String = "", val year: String = "")
@Keep data class NavamsaPlanet(val name: String = "", val signName: String = "")
@Keep data class NavamsaData(val planets: List<NavamsaPlanet>? = null)
@Keep data class KPSignificators(val planetView: List<KPPlanet>? = null, val houseView: List<KPHouse>? = null)
@Keep data class KPPlanet(val name: String = "", val levelA: List<Int> = emptyList(), val levelB: List<Int> = emptyList(), val levelC: List<Int> = emptyList(), val levelD: List<Int> = emptyList())
@Keep data class KPHouse(val house: Int = 1, val level1: List<String> = emptyList(), val level2: List<String> = emptyList(), val level3: List<String> = emptyList(), val level4: List<String> = emptyList(), val lord: String = "")

class VipChartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val birthDataStr = intent.getStringExtra("birthData") ?: "{}"
        val birthData = JSONObject(birthDataStr)

        setContent {
            CosmicAppTheme {
                VipChartScreen(birthData) { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipChartScreen(birthData: JSONObject, onBack: () -> Unit) {
    var chartState by remember { mutableStateOf<ChartData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val result = fetchFullChart(birthData)
                chartState = result
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Rasi & Navamsa Charts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ChocolateBrown)
                            Text(birthData.optString("name", "User"), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = ChocolateBrown) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ParchmentLight)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(ParchmentLight)) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChocolateBrown)
                }
            } else if (chartState != null) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ParchmentLight,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ChocolateBrown
                        )
                    }
                ) {
                    val tabs = listOf("ராசி கட்டங்கள்", "கிரக நிலைகள்", "சுப அசுப கிரகங்கள்", "தசா புத்திகள்", "பஞ்சாங்கம்")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 13.sp,
                                    fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if(selectedTab == index) ChocolateBrown else Color.Gray
                                )
                            }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> ChartsTab(chartState!!, birthData)
                        1 -> PlanetsTab(chartState!!)
                        2 -> SubhaAsubhaTab(chartState!!)
                        3 -> DashaListTab(chartState!!.dasha)
                        4 -> PanchangaTab(chartState!!)
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("சார்ட் லோடு செய்வதில் தோல்வி (Failed to load chart)", color = Color.Red, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            isLoading = true
                            scope.launch {
                                try {
                                    val result = fetchFullChart(birthData)
                                    chartState = result
                                } finally {
                                    isLoading = false
                                }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartsTab(data: ChartData, birthData: JSONObject) {
    val currentAscSign = data.planets.find { it.name.equals("Ascendant", ignoreCase = true) || it.name.equals("Lagna", ignoreCase = true) }?.signName
        ?: data.houses.ascendantDetails.signName.takeIf { it.isNotBlank() }
        ?: "Aries"

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

        Text("ராசி கட்டம் (Rasi)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF5D1212), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        SouthIndianGridEnhanced(
            planets = data.planets,
            houses = null,
            ascSign = currentAscSign,
            title = "ராசி",
            isBhava = false
        )

        Spacer(Modifier.height(32.dp))

        // Navamsa Chart
        val navamsaPlanets = data.navamsa?.planets ?: emptyList()
        val navamsaAscSign = navamsaPlanets.find { it.name == "Ascendant" || it.name == "As" }?.signName ?: ""
        val filteredNavamsaPlanets = navamsaPlanets.filter { it.name != "Ascendant" && it.name != "As" && it.signName.isNotEmpty() }

        Text("நவாம்ச கட்டம் (Navamsa)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF5D1212), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        NavamsaSouthIndianGrid(
            planets = filteredNavamsaPlanets,
            ascSign = navamsaAscSign,
            title = "நவாம்சம்"
        )

        Spacer(Modifier.height(32.dp))

        Text("பாவக கட்டம் (Bhava)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF5D1212), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        SouthIndianGridEnhanced(
            planets = data.planets, // In Bhava, planet positions might be different, but if we don't have separate bhava planets, we use Rasi planets
            houses = data.houses.details,
            ascSign = currentAscSign,
            title = "பாவகம்",
            isBhava = true
        )

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SouthIndianGridEnhanced(
    planets: List<Planet>,
    houses: List<HouseDetail>?,
    ascSign: String,
    title: String,
    isBhava: Boolean
) {
    val signNames = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
    val gridMap = listOf(11, 0, 1, 2, 10, -1, -1, 3, 9, -1, -1, 4, 8, 7, 6, 5)
    
    val romanNumerals = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(4.dp))
    ) {
        // Outer Border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color(0xFFFF8C00), RoundedCornerShape(4.dp))
        )

        // Grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cellW = w / 4
            val cellH = h / 4

            // Vertical lines
            for (i in 1..3) {
                if (i == 2) {
                    drawLine(Color(0xFFFF8C00), Offset(i * cellW, 0f), Offset(i * cellW, cellH), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0xFFFF8C00), Offset(i * cellW, 3 * cellH), Offset(i * cellW, h), strokeWidth = 1.dp.toPx())
                } else {
                    drawLine(Color(0xFFFF8C00), Offset(i * cellW, 0f), Offset(i * cellW, h), strokeWidth = 1.dp.toPx())
                }
            }
            // Horizontal lines
            for (i in 1..3) {
                if (i == 2) {
                    drawLine(Color(0xFFFF8C00), Offset(0f, i * cellH), Offset(cellW, i * cellH), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0xFFFF8C00), Offset(3 * cellW, i * cellH), Offset(w, i * cellH), strokeWidth = 1.dp.toPx())
                } else {
                    drawLine(Color(0xFFFF8C00), Offset(0f, i * cellH), Offset(w, i * cellH), strokeWidth = 1.dp.toPx())
                }
            }
            
            // Central Border (Thicker orange around center 2x2 hole)
            val rectPath = Path().apply {
                moveTo(cellW, cellH)
                lineTo(3 * cellW, cellH)
                lineTo(3 * cellW, 3 * cellH)
                lineTo(cellW, 3 * cellH)
                close()
            }
            drawPath(rectPath, Color(0xFFFF8C00), style = Stroke(width = 1.dp.toPx()))
        }

        // Contents
        Column(Modifier.fillMaxSize()) {
            for (row in 0..3) {
                Row(Modifier.weight(1f)) {
                    for (col in 0..3) {
                        val pos = row * 4 + col
                        val signIdx = gridMap[pos]

                        if (signIdx != -1) {
                            val signEn = signNames[signIdx]
                            var showDialog by remember { mutableStateOf(false) }

                            if (showDialog) {
                                val tamilName = signTamil[signEn] ?: signEn
                                val details = rasiClassifications[signEn]
                                if (details != null) {
                                    AlertDialog(
                                        onDismissRequest = { showDialog = false },
                                        title = {
                                            Text(
                                                text = "$tamilName",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF5D1212)
                                            )
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row {
                                                    Text("அதிபதி: ", fontWeight = FontWeight.Bold)
                                                    Text("${details.lordTa}")
                                                }
                                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                Column {
                                                    Text("சுபர்கள்:", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                                    Text(details.subhargal)
                                                }
                                                Column {
                                                    Text("பாபர்கள்:", fontWeight = FontWeight.Bold, color = Color(0xFFE1353C))
                                                    Text(details.paabargal)
                                                }
                                                Column {
                                                    Text("மாரகர்:", fontWeight = FontWeight.Bold, color = Color.Gray)
                                                    Text(details.maaragar)
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showDialog = false }) {
                                                Text("சரி", fontWeight = FontWeight.Bold, color = Color(0xFF5D1212))
                                            }
                                        }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { showDialog = true }
                            ) {
                                val isAsc = signEn == ascSign
                                
                                // Draw Green Border for Ascendant
                                if (isAsc) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(2.dp, Color(0xFF4CAF50))
                                    )
                                }

                                Column(
                                    Modifier.fillMaxSize().padding(2.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Top section (House Num + Degree for Bhava)
                                    if (isBhava && houses != null) {
                                        // Find which house falls in this sign. A sign could have 0, 1, or 2 houses. We'll find the first one.
                                        val houseIndex = houses.indexOfFirst { it.signName == signEn }
                                        if (houseIndex != -1) {
                                            val h = houses[houseIndex]
                                            val hDeg = h.degreeFormatted ?: ""
                                            val topText = hDeg
                                            if (topText.isNotEmpty()) {
                                                Text(
                                                    text = topText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0A1172)
                                                )
                                            }
                                        }
                                    }

                                    // Planets in this sign
                                    val signPlanets = planets.filter { it.signName == signEn && !it.name.equals("Ascendant", ignoreCase = true) && !it.name.equals("Lagna", ignoreCase = true) }
                                    signPlanets.forEach { p ->
                                        val abbr = planetAbbrTamil[p.name] ?: p.name.take(3)
                                        val deg = p.degreeFormatted ?: ""
                                        val text = if (deg.isNotEmpty()) "$abbr $deg" else abbr
                                        Text(
                                            text = text,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0A1172)
                                        )
                                    }
                                    
                                    // Ascendant Label
                                    if (isAsc) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("லக்", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                // Empty or Central Info Displays
                            }
                        }
                    }
                }
            }
        }

        // Overlay central text over the 2x2 hole
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0A1172))
        }
    }
}

@Composable
fun NavamsaSouthIndianGrid(
    planets: List<NavamsaPlanet>,
    ascSign: String,
    title: String
) {
    val signNames = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
    val gridMap = listOf(11, 0, 1, 2, 10, -1, -1, 3, 9, -1, -1, 4, 8, 7, 6, 5)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(4.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize().border(2.dp, Color(0xFFFF8C00), RoundedCornerShape(4.dp)))

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cellW = w / 4
            val cellH = h / 4
            for (i in 1..3) {
                if (i == 2) {
                    drawLine(Color(0xFFFF8C00), Offset(i * cellW, 0f), Offset(i * cellW, cellH), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0xFFFF8C00), Offset(i * cellW, 3 * cellH), Offset(i * cellW, h), strokeWidth = 1.dp.toPx())
                } else {
                    drawLine(Color(0xFFFF8C00), Offset(i * cellW, 0f), Offset(i * cellW, h), strokeWidth = 1.dp.toPx())
                }
            }
            for (i in 1..3) {
                if (i == 2) {
                    drawLine(Color(0xFFFF8C00), Offset(0f, i * cellH), Offset(cellW, i * cellH), strokeWidth = 1.dp.toPx())
                    drawLine(Color(0xFFFF8C00), Offset(3 * cellW, i * cellH), Offset(w, i * cellH), strokeWidth = 1.dp.toPx())
                } else {
                    drawLine(Color(0xFFFF8C00), Offset(0f, i * cellH), Offset(w, i * cellH), strokeWidth = 1.dp.toPx())
                }
            }
            val rectPath = Path().apply {
                moveTo(cellW, cellH); lineTo(3 * cellW, cellH); lineTo(3 * cellW, 3 * cellH); lineTo(cellW, 3 * cellH); close()
            }
            drawPath(rectPath, Color(0xFFFF8C00), style = Stroke(width = 1.dp.toPx()))
        }

        Column(Modifier.fillMaxSize()) {
            for (row in 0..3) {
                Row(Modifier.weight(1f)) {
                    for (col in 0..3) {
                        val pos = row * 4 + col
                        val signIdx = gridMap[pos]

                        if (signIdx != -1) {
                            val signEn = signNames[signIdx]
                            var showDialog by remember { mutableStateOf(false) }

                            if (showDialog) {
                                val tamilName = signTamil[signEn] ?: signEn
                                val details = rasiClassifications[signEn]
                                if (details != null) {
                                    AlertDialog(
                                        onDismissRequest = { showDialog = false },
                                        title = {
                                            Text(
                                                text = "$tamilName",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF5D1212)
                                            )
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row {
                                                    Text("அதிபதி: ", fontWeight = FontWeight.Bold)
                                                    Text("${details.lordTa}")
                                                }
                                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                Column {
                                                    Text("சுபர்கள்:", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                                    Text(details.subhargal)
                                                }
                                                Column {
                                                    Text("பாபர்கள்:", fontWeight = FontWeight.Bold, color = Color(0xFFE1353C))
                                                    Text(details.paabargal)
                                                }
                                                Column {
                                                    Text("மாரகர்:", fontWeight = FontWeight.Bold, color = Color.Gray)
                                                    Text(details.maaragar)
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showDialog = false }) {
                                                Text("சரி", fontWeight = FontWeight.Bold, color = Color(0xFF5D1212))
                                            }
                                        }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { showDialog = true }
                            ) {
                                val isAsc = signEn == ascSign
                                if (isAsc) {
                                    Box(modifier = Modifier.fillMaxSize().border(2.dp, Color(0xFF4CAF50)))
                                }
                                Column(
                                    Modifier.fillMaxSize().padding(2.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val signPlanets = planets.filter { it.signName == signEn }
                                    signPlanets.forEach { p ->
                                        val abbr = planetAbbrTamil[p.name] ?: p.name.take(3)
                                        Text(text = abbr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A1172))
                                    }
                                    if (isAsc) {
                                        Box(
                                            modifier = Modifier.padding(top = 4.dp)
                                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("லக்", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                // Empty or Central Info Displays
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0A1172))
        }
    }
}

fun getMonthName(m: Int): String = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[m]

fun getPlanetStatusTamil(planetName: String, signName: String): String {
    return when (planetName) {
        "Sun" -> when (signName) { "Aries" -> "உச்சம்"; "Libra" -> "நீசம்"; "Leo" -> "ஆட்சி"; "Sagittarius", "Pisces", "Scorpio", "Cancer" -> "நட்பு"; "Taurus", "Capricorn", "Aquarius" -> "பகை"; "Gemini", "Virgo" -> "சமம்"; else -> "சமம்" }
        "Moon" -> when (signName) { "Taurus" -> "உச்சம்"; "Scorpio" -> "நீசம்"; "Cancer" -> "ஆட்சி"; "Aries", "Leo", "Sagittarius", "Pisces" -> "நட்பு"; "Gemini", "Virgo", "Capricorn", "Aquarius", "Libra" -> "சமம்"; else -> "சமம்" }
        "Mars" -> when (signName) { "Capricorn" -> "உச்சம்"; "Cancer" -> "நீசம்"; "Aries", "Scorpio" -> "ஆட்சி"; "Leo", "Sagittarius", "Pisces" -> "நட்பு"; "Gemini", "Virgo" -> "பகை"; "Taurus", "Libra", "Aquarius" -> "சமம்"; else -> "சமம்" }
        "Mercury" -> when (signName) { "Virgo" -> "உச்சம்/ஆட்சி"; "Pisces" -> "நீசம்"; "Gemini" -> "ஆட்சி"; "Taurus", "Leo", "Libra" -> "நட்பு"; "Cancer" -> "பகை"; "Aries", "Scorpio", "Sagittarius", "Capricorn", "Aquarius" -> "சமம்"; else -> "சமம்" }
        "Jupiter" -> when (signName) { "Cancer" -> "உச்சம்"; "Capricorn" -> "நீசம்"; "Sagittarius", "Pisces" -> "ஆட்சி"; "Aries", "Leo", "Scorpio" -> "நட்பு"; "Taurus", "Gemini", "Virgo", "Libra" -> "பகை"; "Aquarius" -> "சமம்"; else -> "சமம்" }
        "Venus" -> when (signName) { "Pisces" -> "உச்சம்"; "Virgo" -> "நீசம்"; "Taurus", "Libra" -> "ஆட்சி"; "Gemini", "Capricorn", "Aquarius" -> "நட்பு"; "Cancer", "Leo" -> "பகை"; "Aries", "Scorpio", "Sagittarius" -> "சமம்"; else -> "சமம்" }
        "Saturn" -> when (signName) { "Libra" -> "உச்சம்"; "Aries" -> "நீசம்"; "Capricorn", "Aquarius" -> "ஆட்சி"; "Taurus", "Gemini", "Virgo" -> "நட்பு"; "Cancer", "Leo", "Scorpio" -> "பகை"; "Sagittarius", "Pisces" -> "சமம்"; else -> "சமம்" }
        "Rahu" -> when (signName) { "Taurus" -> "உச்சம்"; "Scorpio" -> "நீசம்"; "Virgo", "Aquarius" -> "ஆட்சி"; else -> "நட்பு" }
        "Ketu" -> when (signName) { "Scorpio" -> "உச்சம்"; "Taurus" -> "நீசம்"; "Pisces", "Aries" -> "ஆட்சி"; else -> "நட்பு" }
        else -> "-"
    }
}

@Composable
fun PlanetsTab(data: ChartData) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("கிரக நிலைகள் (நவக்கிரகங்கள்)", fontWeight = FontWeight.Bold, color = ChocolateBrown, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        // New Precise Table Grid with Nakshatra Lord / Sara Nathan
        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE87A1E)).padding(vertical = 8.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    "கிரகம்" to 1.1f,
                    "நட்சத்திரம்" to 1.2f,
                    "பாதம்" to 0.6f,
                    "ந. அதிபதி" to 1.1f,
                    "ராசி" to 1.0f,
                    "நிலை" to 0.9f
                ).forEach { (head, weight) ->
                    Text(
                        text = head,
                        modifier = Modifier.weight(weight),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Rows
            data.planets.forEach { planet ->
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth().background(ParchmentBase).padding(vertical = 10.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Planet Name (Red)
                    Row(Modifier.weight(1.1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = planetTamil[planet.name] ?: planet.name,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Nakshatra (Blue)
                    Text(
                        text = nakshatraTamil[planet.nakshatra] ?: planet.nakshatra,
                        color = Color.Blue,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.Center
                    )

                    // Pada (Blue)
                    Text(
                        text = planet.nakshatraPada.toString(),
                        color = Color.Blue,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(0.6f),
                        textAlign = TextAlign.Center
                    )

                    // Nakshatra Lord / Sara Nathan (Blue)
                    val starLordName = planet.starLord?.let { planetTamil[it] ?: it }
                        ?: nakshatraLordTamil[planet.nakshatra]
                        ?: nakshatraLordTamil[nakshatraTamil[planet.nakshatra] ?: ""]
                        ?: "-"
                    Text(
                        text = starLordName,
                        color = Color.Blue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.1f),
                        textAlign = TextAlign.Center
                    )

                    // Rasi Sign (Blue)
                    Text(
                        text = signTamil[planet.signName] ?: planet.signName,
                        color = Color.Blue,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1.0f),
                        textAlign = TextAlign.Center
                    )

                    // Status (Blue)
                    Text(
                        text = getPlanetStatusTamil(planet.name, planet.signName),
                        color = Color.Blue,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SubhaAsubhaTab(data: ChartData) {
    val ascendantPlanet = data.planets.find { it.name.equals("Ascendant", ignoreCase = true) || it.name.equals("Lagna", ignoreCase = true) }
    val lagnaSignEn = ascendantPlanet?.signName
        ?: data.houses.ascendantDetails.signName.takeIf { it.isNotBlank() }
        ?: "Aries"
    val lagnaSignTa = signTamil[lagnaSignEn] ?: lagnaSignEn
    val details = rasiClassifications[lagnaSignEn]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ChocolateBrown.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ChocolateBrown.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "உங்கள் லக்னம்: $lagnaSignTa",
                    fontWeight = FontWeight.Bold,
                    color = ChocolateBrown,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "லக்ன அடிப்படையில் கிரகங்களின் ஆதிபத்தியங்கள் பின்வருமாறு கணக்கிடப்பட்டுள்ளது:",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (details != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE87A1E))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "கிரக வகைப்பாடு",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "கிரகங்கள்",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.End
                    )
                }

                // Row 1: அதிபதி
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ParchmentBase)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "லக்னாதிபதி",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = details.lordTa,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ChocolateBrown,
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.End
                    )
                }
                
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                // Row 2: சுபர்கள்
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ParchmentLight)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "சுப கிரகங்கள்",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = details.subhargal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.End
                    )
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                // Row 3: பாபர்கள்
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ParchmentBase)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "பாப கிரகங்கள்",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFE1353C),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = details.paabargal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFE1353C),
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.End
                    )
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                // Row 4: மாரகர்
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ParchmentLight)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "மாரக கிரகங்கள்",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = details.maaragar,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun PlanetDetailSub(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun DashaListTab(mahadashas: List<DashaPeriod>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(Modifier.fillMaxWidth().background(ChocolateBrown).padding(16.dp)) {
                Text("விம்சோத்தரி தசா புத்தி விபரங்கள்", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        items(mahadashas) { md ->
            DashaNodeInternal(md)
        }
    }
}

@Composable
fun DashaNodeInternal(period: DashaPeriod) {
    var expanded by remember { mutableStateOf(false) }
    val hasSub = !period.subPeriods.isNullOrEmpty()
    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val isCurrent = todayStr >= period.start.take(10) && todayStr <= period.end.take(10)

    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isCurrent) Color(0xFFE1353C).copy(alpha = 0.1f) else Color.Transparent)
                .clickable(enabled = hasSub) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val levelIndent = (period.level - 1) * 20
            Spacer(Modifier.width(levelIndent.dp))

            // Icon/Prefix based on level
            val iconColor = when(period.level) {
                1 -> ChocolateBrown
                2 -> Color(0xFF2E7D32)
                3 -> Color(0xFF1976D2)
                else -> Color.DarkGray
            }

            Box(Modifier.size(32.dp).background(iconColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Text(planetAbbrTamil[period.lord] ?: period.lord.take(2), color = iconColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "${planetTamil[period.lord] ?: period.lord} " + (dashaLevelTamil[period.level] ?: when(period.level) {
                        1 -> "மகா தசை"
                        2 -> "புத்தி"
                        3 -> "அந்தரம்"
                        4 -> "பிரத்யந்தரம்"
                        else -> "சூட்சமம்"
                    }),
                    fontWeight = if(period.level == 1) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if(period.level == 1) 16.sp else 14.sp,
                    color = Color.DarkGray
                )
                Text("${period.start.take(10).replace("-", ".")} - ${period.end.take(10).replace("-", ".")}", fontSize = 11.sp, color = Color.Gray)
            }

            if (hasSub) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }

        if (expanded && hasSub) {
            period.subPeriods?.forEach { child ->
                DashaNodeInternal(child)
            }
            Divider(Modifier.padding(start = ((period.level) * 20).dp), color = Color.Gray.copy(0.1f))
        }
        if (period.level == 1) {
            Divider(color = Color.LightGray.copy(0.4f))
        }
    }
}

fun applyKp12thLordDefeatRule(connections: List<Int>): List<Int> {
    val result = mutableListOf<Int>()
    for (bhava in connections) {
        val twelth = if (bhava == 1) 12 else bhava - 1
        if (!connections.contains(twelth)) {
            result.add(bhava)
        }
    }
    return result
}

fun getKPKBPlanetConnections(planetName: String, data: ChartData): String {
    val planet = data.planets.find { it.name.equals(planetName, ignoreCase = true) } ?: return "-"
    
    // 1. Houses signified by the Star-Lord (Primary)
    val starLordName = planet.starLord ?: ""
    val starLordPlanet = data.planets.find { it.name.equals(starLordName, ignoreCase = true) }
    
    val primaryConnections = mutableListOf<Int>()
    if (starLordPlanet != null) {
        val starLordOccupies = starLordPlanet.house
        val starLordOwns = data.houses.details.mapIndexedNotNull { index, h -> 
            if (h.signLord.equals(starLordName, ignoreCase = true)) index + 1 else null 
        }
        if (starLordOccupies != 0) primaryConnections.add(starLordOccupies)
        primaryConnections.addAll(starLordOwns)
    }
    
    // 2. Houses signified by the Planet itself (Secondary)
    val planetOccupies = planet.house
    val planetOwns = data.houses.details.mapIndexedNotNull { index, h -> 
        if (h.signLord.equals(planet.name, ignoreCase = true)) index + 1 else null 
    }
    
    val secondaryConnections = mutableListOf<Int>()
    if (planetOccupies != 0) secondaryConnections.add(planetOccupies)
    secondaryConnections.addAll(planetOwns)
    
    val union = (primaryConnections + secondaryConnections).distinct().sorted()
    if (union.isEmpty()) return "-"
    return union.joinToString(", ")
}

fun getKPKBHouseConnections(houseIndex: Int, data: ChartData): String {
    val house = data.houses.details.getOrNull(houseIndex) ?: return ""
    
    // 1. Cuspal Sub-Lord of the requested Bhava
    val subLordName = house.subLord ?: return "-"
    val subLordPlanet = data.planets.find { it.name.equals(subLordName, ignoreCase = true) } ?: return "-"
    
    // 2. Star-Lord of that Sub-Lord
    val starLordName = subLordPlanet.starLord ?: return "-"
    val starLordPlanet = data.planets.find { it.name.equals(starLordName, ignoreCase = true) } ?: return "-"
    
    // 3. Houses signified by the Star-Lord (Primary connections)
    val starLordOccupies = starLordPlanet.house
    val starLordOwns = data.houses.details.mapIndexedNotNull { index, h -> 
        if (h.signLord.equals(starLordName, ignoreCase = true)) index + 1 else null 
    }
    
    val primaryConnections = mutableListOf<Int>()
    if (starLordOccupies != 0) primaryConnections.add(starLordOccupies)
    primaryConnections.addAll(starLordOwns)
    
    // 4. Houses signified by the Sub-Lord (for confirmation)
    val subLordOccupies = subLordPlanet.house
    val subLordOwns = data.houses.details.mapIndexedNotNull { index, h -> 
        if (h.signLord.equals(subLordName, ignoreCase = true)) index + 1 else null 
    }
    
    val secondaryConnections = mutableListOf<Int>()
    if (subLordOccupies != 0) secondaryConnections.add(subLordOccupies)
    secondaryConnections.addAll(subLordOwns)
    
    val union = (primaryConnections + secondaryConnections).distinct().sorted()
    if (union.isEmpty()) return "-"
    return union.joinToString(", ")
}

@Composable
fun IndicatorsTab(data: ChartData) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("கிரக குறி காட்டிகள்", fontWeight = FontWeight.Bold, color = Color(0xFF5D1212), fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0D5C9))) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9F0E6)).padding(8.dp)) {
                listOf("கிரகம்", "நட்சத்திரம்\nபாதம்", "நட்சத்திர\nஅதிபதி", "பாவ\nதொடர்பு").forEach { head ->
                    Text(
                        text = head,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF5D1212),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            val displayPlanets = listOf("Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu", "Mandi")
            data.planets.filter { it.name in displayPlanets }.forEachIndexed { index, planet ->
                val planetTa = planetTamil[planet.name] ?: planet.name
                val planetHouse = planet.house
                val nak = "${planet.nakshatra} ${planet.nakshatraPada}"
                
                val starLordEn = planet.starLord ?: ""
                val starLordTa = planetTamil[starLordEn] ?: starLordEn
                val starLordPlanet = data.planets.find { it.name.equals(starLordEn, ignoreCase = true) }
                val starLordHouse = starLordPlanet?.house?.toString() ?: ""
                
                val col1 = "$planetTa $planetHouse"
                val col3 = if (starLordTa.isNotEmpty()) "$starLordTa $starLordHouse ல்" else "-"
                
                val connection = getKPKBPlanetConnections(planet.name, data)

                HorizontalDivider(color = Color(0xFFE0D5C9))
                Row(modifier = Modifier.fillMaxWidth().background(if (index % 2 == 0) Color.White else Color(0xFFFAF6F2)).padding(vertical = 10.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(col1, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(nak, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    Text(col3, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(connection, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("பாவக குறி காட்டிகள்", fontWeight = FontWeight.Bold, color = Color(0xFF5D1212), fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0D5C9))) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9F0E6)).padding(8.dp)) {
                listOf("பாவ\nஆரம்ப\nமுனை", "நட்சத்திர\nபாதம்", "உப\nஅதிபதி", "நின்ற\nநட்சத்திர\nஅதிபதி", "பாவ\nதொடர்பு").forEach { head ->
                    Text(
                        text = head,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF5D1212),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            data.houses.details.forEachIndexed { index, house ->
                val bhavaNum = index + 1
                val nakshatra = "${house.nakshatra ?: ""} ${house.nakshatraPada ?: ""}"
                
                // 1. Cuspal Sub-Lord of the requested Bhava
                val subLordEn = house.subLord ?: ""
                val subLordTa = planetTamil[subLordEn] ?: subLordEn
                val subLordPlanet = data.planets.find { it.name.equals(subLordEn, ignoreCase = true) }
                val subLordHouse = subLordPlanet?.house?.toString() ?: ""
                
                // 2. Star-Lord of that Sub-Lord
                val starLordEn = subLordPlanet?.starLord ?: ""
                val starLordTa = planetAbbrTamil[starLordEn] ?: planetTamil[starLordEn] ?: starLordEn
                val starLordPlanet = data.planets.find { it.name.equals(starLordEn, ignoreCase = true) }
                val starLordHouse = starLordPlanet?.house?.toString() ?: ""

                val col3 = if (subLordTa.isNotEmpty()) "$subLordTa $subLordHouse" else "-"
                val col4 = if (starLordTa.isNotEmpty()) "$starLordTa $starLordHouse" else "-"
                
                val connection = getKPKBHouseConnections(index, data)
                
                HorizontalDivider(color = Color(0xFFE0D5C9))
                Row(modifier = Modifier.fillMaxWidth().background(if (index % 2 == 0) Color.White else Color(0xFFFAF6F2)).padding(vertical = 10.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(bhavaNum.toString(), color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(nakshatra, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    Text(col3, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(col4, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(connection, color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun PanchangaTab(data: ChartData) {
    val p = data.panchanga
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("பஞ்சாங்கம்", fontWeight = FontWeight.Bold, color = ChocolateBrown, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))

        val items = listOf(
            "திதி" to (p.tithi?.name ?: "-"),
            "நட்சத்திரம்" to (p.nakshatra?.name ?: "-"),
            "யோகம்" to (p.yoga?.name ?: "-"),
            "கரணம்" to (p.karana?.name ?: "-"),
            "வாரம்" to (p.vara?.name ?: "-"),
            "சூரிய உதயம்" to (p.sunrise ?: "-"),
            "சூரிய அஸ்தமனம்" to (p.sunset ?: "-"),
            "சந்திர ராசி" to (signTamil[p.moonSign] ?: p.moonSign ?: "-"),
            "சூரிய ராசி" to (signTamil[p.sunSign] ?: p.sunSign ?: "-")
        )

        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray)) {
            items.forEachIndexed { i, (label, value) ->
                if (i > 0) HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (i % 2 == 0) ParchmentBase else ParchmentLight)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.DarkGray)
                    Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ChocolateBrown, textAlign = TextAlign.End)
                }
            }
        }

        // Tamil Date
        data.tamilDate?.let { td ->
            Spacer(Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChocolateBrown.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("தமிழ் தேதி", fontWeight = FontWeight.Bold, color = ChocolateBrown, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("${td.day} ${td.month} ${td.year}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.DarkGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Transit Table
        if (data.transits.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("தற்போதைய கோச்சாரம்", fontWeight = FontWeight.Bold, color = ChocolateBrown, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray)) {
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE87A1E)).padding(8.dp)) {
                    listOf("கிரகம்", "ராசி", "வக்கிரம்").forEach { h ->
                        Text(h, modifier = Modifier.weight(1f), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
                data.transits.forEachIndexed { i, t ->
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.4f))
                    Row(modifier = Modifier.fillMaxWidth().background(if (i % 2 == 0) ParchmentBase else ParchmentLight).padding(vertical = 10.dp, horizontal = 4.dp)) {
                        Text(planetTamil[t.name] ?: t.name, modifier = Modifier.weight(1f), color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(signTamil[t.signName] ?: t.signName, modifier = Modifier.weight(1f), color = Color.Blue, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(if (t.isRetrograde) "வக்கிரம்" else "-", modifier = Modifier.weight(1f), color = if (t.isRetrograde) Color.Red else Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private suspend fun fetchFullChart(birthData: JSONObject): ChartData? = withContext(Dispatchers.IO) {
    try {
        var lat = birthData.optDouble("latitude")
        var lng = birthData.optDouble("longitude")
        if (lat == 0.0 || lat.isNaN() || lng == 0.0 || lng.isNaN()) {
            lat = 13.0827
            lng = 80.2707
        }
        val y = birthData.optInt("year").let { if (it <= 1900) 1995 else it }
        val m = birthData.optInt("month").let { if (it <= 0 || it > 12) 1 else it }
        val d = birthData.optInt("day").let { if (it <= 0 || it > 31) 1 else it }
        val h = birthData.optInt("hour", 12)
        val min = birthData.optInt("minute", 0)

        val payload = com.google.gson.JsonObject().apply {
            addProperty("date", String.format("%04d-%02d-%02d", y, m, d))
            addProperty("time", String.format("%02d:%02d", h, min))
            addProperty("lat", lat)
            addProperty("lng", lng)
            addProperty("timezone", birthData.optDouble("timezone", 5.5))
        }

        android.util.Log.d("VipChart", "Fetching chart with payload: $payload")

        val response = com.astroeleven.app.data.api.ApiClient.api.getRasiEngBirthChart(payload)
        android.util.Log.d("VipChart", "Response code: ${response.code()}, successful: ${response.isSuccessful}")

        if (response.isSuccessful && response.body() != null) {
            val jsonString = response.body().toString()
            android.util.Log.d("VipChart", "Response JSON (first 300): ${jsonString.take(300)}")
            
            // 1. Try parsing with Gson
            try {
                val chartResponse = Gson().fromJson(jsonString, ChartResponse::class.java)
                if (chartResponse != null && chartResponse.success && chartResponse.data != null && chartResponse.data.planets.isNotEmpty()) {
                    android.util.Log.d("VipChart", "Parsed success via Gson, planets: ${chartResponse.data.planets.size}")
                    return@withContext chartResponse.data
                }
            } catch (e: Exception) {
                android.util.Log.w("VipChart", "Gson parse failed, falling back to manual JSON parse: ${e.message}")
            }

            // 2. Robust Manual JSONObject Parsing Fallback (Guaranteed to NEVER fail even under ProGuard/R8)
            try {
                val rootJson = JSONObject(jsonString)
                val dataObj = rootJson.optJSONObject("data") ?: rootJson
                
                val planetsList = mutableListOf<Planet>()
                val planetsArr = dataObj.optJSONArray("planets")
                if (planetsArr != null) {
                    for (i in 0 until planetsArr.length()) {
                        val p = planetsArr.getJSONObject(i)
                        planetsList.add(
                            Planet(
                                name = p.optString("name", ""),
                                signName = p.optString("signName", p.optString("sign", "Aries")),
                                signIndex = p.optInt("signIndex", 0),
                                house = p.optInt("house", 1),
                                nakshatra = p.optString("nakshatra", p.optString("nakshatraName", "")),
                                nakshatraPada = p.optInt("nakshatraPada", 1),
                                degreeFormatted = p.optString("degreeFormatted", null),
                                signLord = p.optString("signLord", null),
                                starLord = p.optString("starLord", null),
                                subLord = p.optString("subLord", null),
                                isRetrograde = p.optBoolean("isRetrograde", false),
                                isCombust = p.optBoolean("isCombust", false)
                            )
                        )
                    }
                }

                // Houses
                val housesObj = dataObj.optJSONObject("houses")
                val ascDetailsObj = housesObj?.optJSONObject("ascendantDetails")
                val ascSign = ascDetailsObj?.optString("signName", "")?.takeIf { it.isNotBlank() }
                    ?: planetsList.find { it.name.equals("Ascendant", true) }?.signName
                    ?: "Aries"

                if (!planetsList.any { it.name.equals("Ascendant", true) }) {
                    planetsList.add(0, Planet(name = "Ascendant", signName = ascSign))
                }
                
                val houseDetailsList = mutableListOf<HouseDetail>()
                val detailsArr = housesObj?.optJSONArray("details")
                if (detailsArr != null) {
                    for (i in 0 until detailsArr.length()) {
                        val d = detailsArr.getJSONObject(i)
                        houseDetailsList.add(
                            HouseDetail(
                                signName = d.optString("signName", "Aries"),
                                signAbbr = d.optString("signAbbr", null),
                                nakshatra = d.optString("nakshatra", null),
                                nakshatraPada = d.optInt("nakshatraPada", 1),
                                signLord = d.optString("signLord", null),
                                starLord = d.optString("starLord", null),
                                subLord = d.optString("subLord", null),
                                degreeFormatted = d.optString("degreeFormatted", null)
                            )
                        )
                    }
                }
                
                val houseData = HouseData(
                    details = houseDetailsList,
                    ascendantDetails = HouseDetail(signName = ascSign)
                )

                // Navamsa
                val navamsaObj = dataObj.optJSONObject("navamsa")
                val navamsaPlanets = mutableListOf<NavamsaPlanet>()
                val navArr = navamsaObj?.optJSONArray("planets")
                if (navArr != null) {
                    for (i in 0 until navArr.length()) {
                        val np = navArr.getJSONObject(i)
                        navamsaPlanets.add(NavamsaPlanet(np.optString("name"), np.optString("signName")))
                    }
                }

                // Dasha
                val dashaArr = dataObj.optJSONArray("dasha")
                val dashaList = mutableListOf<DashaPeriod>()
                if (dashaArr != null) {
                    for (i in 0 until dashaArr.length()) {
                        val dItem = dashaArr.getJSONObject(i)
                        dashaList.add(
                            DashaPeriod(
                                lord = dItem.optString("lord", ""),
                                start = dItem.optString("start", ""),
                                end = dItem.optString("end", ""),
                                level = dItem.optInt("level", 1)
                            )
                        )
                    }
                }

                // Panchanga
                val panchaObj = dataObj.optJSONObject("panchanga")
                val panchangaData = if (panchaObj != null) {
                    Panchanga(
                        tithi = NameObject(panchaObj.optJSONObject("tithi")?.optString("name")),
                        nakshatra = NameObject(panchaObj.optJSONObject("nakshatra")?.optString("name")),
                        yoga = NameObject(panchaObj.optJSONObject("yoga")?.optString("name")),
                        karana = NameObject(panchaObj.optJSONObject("karana")?.optString("name")),
                        vara = NameObject(panchaObj.optJSONObject("vara")?.optString("name")),
                        sunrise = panchaObj.optString("sunrise", null),
                        sunset = panchaObj.optString("sunset", null),
                        moonSign = panchaObj.optString("moonSign", null),
                        sunSign = panchaObj.optString("sunSign", null)
                    )
                } else Panchanga()

                // Tamil Date
                val tdObj = dataObj.optJSONObject("tamilDate")
                val tamilDateData = if (tdObj != null) {
                    TamilDate(
                        day = tdObj.optInt("day", 1),
                        month = tdObj.optString("month", ""),
                        year = tdObj.optString("year", "")
                    )
                } else null

                // Transits
                val transitsArr = dataObj.optJSONArray("transits")
                val transitsList = mutableListOf<Transit>()
                if (transitsArr != null) {
                    for (i in 0 until transitsArr.length()) {
                        val t = transitsArr.getJSONObject(i)
                        transitsList.add(
                            Transit(
                                name = t.optString("name", ""),
                                signName = t.optString("signName", ""),
                                isRetrograde = t.optBoolean("isRetrograde", false)
                            )
                        )
                    }
                }

                if (planetsList.isNotEmpty()) {
                    android.util.Log.d("VipChart", "Successfully parsed ${planetsList.size} planets via manual fallback!")
                    return@withContext ChartData(
                        planets = planetsList,
                        houses = houseData,
                        panchanga = panchangaData,
                        dasha = dashaList,
                        transits = transitsList,
                        tamilDate = tamilDateData,
                        navamsa = NavamsaData(planets = navamsaPlanets)
                    )
                }
            } catch (jsonEx: Exception) {
                android.util.Log.e("VipChart", "Manual JSON parse exception: ${jsonEx.message}", jsonEx)
            }
        } else {
            android.util.Log.e("VipChart", "Error response: ${response.code()} - ${response.errorBody()?.string()}")
        }
        null
    } catch (e: Exception) {
        android.util.Log.e("VipChart", "fetchFullChart exception: ${e.message}", e)
        e.printStackTrace()
        null
    }
}

