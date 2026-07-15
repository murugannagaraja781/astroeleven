package com.astroeleven.app.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astroeleven.app.data.api.ApiClient
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Retrieve app language configuration
        val isTamil = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("is_tamil", false)

        setContent {
            MaterialTheme {
                CalendarScreen(isTamil = isTamil, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(isTamil: Boolean, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Panchangam states
    var tithiName by remember { mutableStateOf("") }
    var nakshatraName by remember { mutableStateOf("") }
    var yogaName by remember { mutableStateOf("") }
    var karanaName by remember { mutableStateOf("") }
    var sunrise by remember { mutableStateOf("06:00") }
    var sunset by remember { mutableStateOf("18:30") }
    var rahuStart by remember { mutableStateOf("07:30") }
    var rahuEnd by remember { mutableStateOf("09:00") }
    var yamaStart by remember { mutableStateOf("10:30") }
    var yamaEnd by remember { mutableStateOf("12:00") }
    var guliStart by remember { mutableStateOf("13:30") }
    var guliEnd by remember { mutableStateOf("15:00") }
    
    // Tamil Date states
    var tamilDay by remember { mutableStateOf(1) }
    var tamilMonth by remember { mutableStateOf("") }
    var tamilYear by remember { mutableStateOf("") }

    val formatIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val formatDisplay = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    // Quotes mapping
    val quotes = listOf(
        Pair("சரியான பாதையை தேர்ந்தெடுத்தால்தான் வாழ்க்கையில் வெற்றியடைய முடியும்.", "Only by choosing the right path can you achieve success in life."),
        Pair("உழைப்பே உயர்வு தரும்.", "Hard work always pays off."),
        Pair("இன்றைய உழைப்பு நாளைய வெற்றி.", "Today's effort is tomorrow's success."),
        Pair("சிந்தனை நன்மையாக இருந்தால் செய்யும் செயலும் நன்மையாகும்.", "If thoughts are pure, actions will be successful."),
        Pair("பொறுமை கடலினும் பெரிது.", "Patience is key to victory."),
        Pair("காலம் பொன் போன்றது.", "Time is precious.")
    )

    val dayOfYear = selectedDate.get(Calendar.DAY_OF_YEAR)
    val activeQuote = quotes[dayOfYear % quotes.size]

    // Traditional mappings based on day of week
    val dayOfWeek = (selectedDate.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7 // 0=Sunday, 1=Monday...
    
    val nallaNeramMap = mapOf(
        0 to Pair("07:30 - 08:30", "15:15 - 16:15"),
        1 to Pair("06:15 - 07:15", "16:45 - 17:45"),
        2 to Pair("07:45 - 08:45", "16:45 - 17:45"),
        3 to Pair("09:15 - 10:15", "16:45 - 17:45"),
        4 to Pair("09:15 - 10:15", "18:15 - 19:15"),
        5 to Pair("09:15 - 10:15", "16:45 - 17:45"),
        6 to Pair("07:45 - 08:45", "17:15 - 18:15")
    )
    val nallaNeram = nallaNeramMap[dayOfWeek] ?: Pair("09:00 - 10:30", "15:00 - 16:30")

    val soolamMap = mapOf(
        0 to Pair("West / மேற்கு", "Jaggery / வெல்லம்"),
        1 to Pair("East / கிழக்கு", "Curd / தயிர்"),
        2 to Pair("North / வடக்கு", "Milk / பால்"),
        3 to Pair("North / வடக்கு", "Milk / பால்"),
        4 to Pair("South / தெற்கு", "Oil / எண்ணெய்"),
        5 to Pair("West / மேற்கு", "Sugar / சர்க்கரை"),
        6 to Pair("East / கிழக்கு", "Sesame Oil / நல்லெண்ணெய்")
    )
    val soolamData = soolamMap[dayOfWeek] ?: Pair("East / கிழக்கு", "Milk / பால்")

    val weekDaysTamil = listOf("ஞாயிறு", "திங்கள்", "செவ்வாய்", "புதன்", "வியாழன்", "வெள்ளி", "சனி")
    val weekDaysEnglish = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val currentDayName = if (isTamil) weekDaysTamil[dayOfWeek] else weekDaysEnglish[dayOfWeek]

    // Fetch details when selectedDate updates
    LaunchedEffect(selectedDate) {
        isLoading = true
        errorMessage = null
        val dateString = formatIso.format(selectedDate.time)
        
        try {
            // 1. Fetch Panchanga
            val panchaBody = JsonObject().apply {
                addProperty("date", dateString)
                addProperty("time", "12:00")
                addProperty("lat", 13.0827)
                addProperty("lng", 80.2707)
                addProperty("timezone", 5.5)
            }
            val panchaResp = ApiClient.api.getPanchanga(panchaBody)
            
            // 2. Fetch Tamil Date
            val tamilBody = JsonObject().apply {
                addProperty("date", dateString)
            }
            val tamilResp = ApiClient.api.getTamilDate(tamilBody)

            if (panchaResp.isSuccessful && panchaResp.body()?.get("success")?.asBoolean == true) {
                val data = panchaResp.body()?.getAsJsonObject("data")
                tithiName = data?.getAsJsonObject("tithi")?.get("name")?.asString ?: "Shukla Pratipada"
                nakshatraName = data?.getAsJsonObject("nakshatra")?.get("name")?.asString ?: "Ashwini"
                yogaName = data?.getAsJsonObject("yoga")?.get("name")?.asString ?: "Vishkumbha"
                karanaName = data?.getAsJsonObject("karana")?.get("name")?.asString ?: "Bava"
                sunrise = data?.get("sunrise")?.asString ?: "06:00"
                sunset = data?.get("sunset")?.asString ?: "18:30"
                rahuStart = data?.getAsJsonObject("rahukalam")?.get("start")?.asString ?: "07:30"
                rahuEnd = data?.getAsJsonObject("rahukalam")?.get("end")?.asString ?: "09:00"
                yamaStart = data?.getAsJsonObject("yamagandam")?.get("start")?.asString ?: "10:30"
                yamaEnd = data?.getAsJsonObject("yamagandam")?.get("end")?.asString ?: "12:00"
                guliStart = data?.getAsJsonObject("gulikai")?.get("start")?.asString ?: "13:30"
                guliEnd = data?.getAsJsonObject("gulikai")?.get("end")?.asString ?: "15:00"
            } else {
                errorMessage = "Failed to load Panchanga calculation"
            }

            if (tamilResp.isSuccessful && tamilResp.body()?.get("success")?.asBoolean == true) {
                val data = tamilResp.body()?.getAsJsonObject("data")
                tamilDay = data?.get("day")?.asInt ?: 1
                tamilMonth = data?.get("month")?.asString ?: "Chithirai"
                tamilYear = data?.get("year")?.asString ?: "Prabhava"
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Network Connection Error"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isTamil) "நித்ரா நாட்காட்டி" else "Astro Daily Calendar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFB3262A))
            )
        },
        containerColor = Color(0xFFFFFDF9)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Date Switcher Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB3262A).copy(alpha = 0.05f))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val prev = selectedDate.clone() as Calendar
                    prev.add(Calendar.DAY_OF_MONTH, -1)
                    selectedDate = prev
                }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Day", tint = Color(0xFFB3262A), modifier = Modifier.size(32.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDisplay.format(selectedDate.time),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFFB3262A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentDayName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2B2516)
                    )
                }

                IconButton(onClick = {
                    val next = selectedDate.clone() as Calendar
                    next.add(Calendar.DAY_OF_MONTH, 1)
                    selectedDate = next
                }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Day", tint = Color(0xFFB3262A), modifier = Modifier.size(32.dp))
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFB3262A))
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { selectedDate = selectedDate.clone() as Calendar },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3262A))
                        ) {
                            Text("Retry / மீண்டும் முயற்சி செய்")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tamil Month & Year Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1353C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val displayMonth = if (isTamil) {
                                when(tamilMonth.lowercase()) {
                                    "chithirai" -> "சித்திரை"
                                    "vaikasi" -> "வைகாசி"
                                    "aani" -> "ஆனி"
                                    "aadi" -> "ஆடி"
                                    "avani" -> "ஆவணி"
                                    "purattasi" -> "புரட்டாசி"
                                    "aippasi" -> "ஐப்பசி"
                                    "karthikai" -> "கார்த்திகை"
                                    "margazhi" -> "மார்கழி"
                                    "thai" -> "தை"
                                    "maasi" -> "மாசி"
                                    "panguni" -> "பங்குனி"
                                    else -> tamilMonth
                                }
                            } else tamilMonth

                            Text(
                                text = "$displayMonth - $tamilDay",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tamilYear,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isTamil) "மேல் நோக்கு நாள்" else "Mel Nookku Naal",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                ),
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Nalla Neram (Auspicious times) Box
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1FDF6)),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().background(Color(0xFF2E7D32), RoundedCornerShape(6.dp)).padding(8.dp)
                            ) {
                                Text(
                                    text = if (isTamil) "நல்ல நேரம்" else "Nalla Neram",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isTamil) "காலை" else "Morning", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text(nallaNeram.first, style = MaterialTheme.typography.bodyMedium)
                                }
                                VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.2f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isTamil) "மாலை" else "Evening", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text(nallaNeram.second, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Rahu Kalam, Gulika, Yamakandam Grid
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEADDBA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Column 1: Rahu
                            Column(
                                modifier = Modifier.weight(1f).padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isTamil) "இராகு" else "Rahu",
                                    color = Color(0xFFB3262A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$rahuStart - $rahuEnd", fontSize = 11.sp, color = Color.DarkGray)
                            }
                            VerticalDivider(modifier = Modifier.height(50.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.2f))
                            
                            // Column 2: Gulika
                            Column(
                                modifier = Modifier.weight(1f).padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isTamil) "குளிகை" else "Gulika",
                                    color = Color(0xFFFDBA16),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$guliStart - $guliEnd", fontSize = 11.sp, color = Color.DarkGray)
                            }
                            VerticalDivider(modifier = Modifier.height(50.dp).width(1.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.2f))
                            
                            // Column 3: Yamakandam
                            Column(
                                modifier = Modifier.weight(1f).padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isTamil) "எமகண்டம்" else "Yamakandam",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$yamaStart - $yamaEnd", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    // Soolam, Parigaram, Suryodayam
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF0)),
                        border = BorderStroke(1.dp, Color(0xFFEADDBA).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (isTamil) "சூலம் (Soolam):" else "Soolam:", fontWeight = FontWeight.Bold)
                                Text(soolamData.first)
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (isTamil) "பரிகாரம் (Parigaram):" else "Parigaram:", fontWeight = FontWeight.Bold)
                                Text(soolamData.second)
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (isTamil) "சூரிய உதயம் (Sunrise):" else "Sunrise:", fontWeight = FontWeight.Bold)
                                Text(sunrise)
                            }
                        }
                    }

                    // Panchanga Core Details List (Tithi, Nakshatra, Yoga, Karana)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEADDBA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Tithi
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isTamil) "திதி (Tithi)" else "Tithi", fontWeight = FontWeight.Bold, color = Color(0xFFB3262A))
                                    Text(tithiName)
                                }
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                            
                            // Nakshatram
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isTamil) "நட்சத்திரம் (Nakshatra)" else "Nakshatra", fontWeight = FontWeight.Bold, color = Color(0xFFB3262A))
                                    Text(nakshatraName)
                                }
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                            
                            // Namayogam
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isTamil) "நாமயோகம் (Yoga)" else "Yoga", fontWeight = FontWeight.Bold, color = Color(0xFFB3262A))
                                    Text(yogaName)
                                }
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                            
                            // Karanam
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isTamil) "கரணம் (Karana)" else "Karana", fontWeight = FontWeight.Bold, color = Color(0xFFB3262A))
                                    Text(karanaName)
                                }
                            }
                        }
                    }

                    // Daily Philosophy Quote Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F5EE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isTamil) "இன்றைய பொன்மொழி" else "Thought of the Day",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB3262A),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isTamil) activeQuote.first else activeQuote.second,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                ),
                                color = Color(0xFF2B2516)
                            )
                        }
                    }
                }
            }
        }
    }
}
