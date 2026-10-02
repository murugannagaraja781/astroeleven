package com.astroeleven.app.ui.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astroeleven.app.data.api.ApiClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * CalendarActivity - Full Astro Eleven Dynamic Daily & Monthly Panchangam Grid Calendar Activity
 */
class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isTamil = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("is_tamil", false)
        val initialTab = intent.getIntExtra("selectedTab", 1)

        setContent {
            MaterialTheme {
                CalendarScreen(isTamil = isTamil, initialTab = initialTab, onBack = { finish() })
            }
        }
    }
}

// Dynamic Monthly Calendar Data Models
data class MuhurthamDay(
    val dayNum: Int,
    val englishText: String,
    val tamilText: String
)

data class FastingItem(
    val typeEn: String,
    val typeTa: String,
    val datesEn: String,
    val datesTa: String,
    val color: Color
)

data class DynamicMonthData(
    val year: Int,
    val monthIndex: Int, // 0-indexed
    val monthNameEn: String,
    val monthNameTa: String,
    val tamilSpanEn: String,
    val tamilSpanTa: String,
    val muhurthamDays: List<MuhurthamDay>,
    val fastingList: List<FastingItem>,
    val amavasaiDays: Set<Int>,
    val pournamiDays: Set<Int>
)

data class CalendarGridCell(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val tamilDateNum: Int,
    val isSunday: Boolean,
    val isMuhurtham: Boolean,
    val isAmavasai: Boolean,
    val isPournami: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(isTamil: Boolean, initialTab: Int = 1, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(initialTab) } // 0 = Daily View, 1 = Monthly View
    
    // --- Daily Calendar States ---
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
    
    var tamilDay by remember { mutableStateOf(1) }
    var tamilMonth by remember { mutableStateOf("") }
    var tamilYear by remember { mutableStateOf("") }

    // --- Dynamic Monthly Calendar States ---
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-indexed
    var showYearDropdown by remember { mutableStateOf(false) }

    var isMonthlyLoading by remember { mutableStateOf(true) }
    var monthlyDataState by remember { mutableStateOf<DynamicMonthData?>(null) }

    val formatIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val formatDisplay = SimpleDateFormat("dd-MM-yyyy", Locale.US)

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
    val dayOfWeek = (selectedDate.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7

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

    // Fetch Daily Panchangam Details
    LaunchedEffect(selectedDate) {
        isLoading = true
        errorMessage = null
        val dateString = formatIso.format(selectedDate.time)
        
        try {
            val panchaBody = JsonObject().apply {
                addProperty("date", dateString)
                addProperty("time", "12:00")
                addProperty("lat", 13.0827)
                addProperty("lng", 80.2707)
                addProperty("timezone", 5.5)
            }
            val panchaResp = ApiClient.api.getPanchanga(panchaBody)
            
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

    // Dynamic Monthly Panchangam Calculation Effect
    LaunchedEffect(selectedYear, selectedMonthIndex) {
        isMonthlyLoading = true
        try {
            val body = JsonObject().apply {
                addProperty("year", selectedYear)
                addProperty("month", selectedMonthIndex + 1)
                addProperty("lat", 13.0827)
                addProperty("lng", 80.2707)
            }
            val resp = ApiClient.api.getMonthlyPanchanga(body)
            if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) {
                val data = resp.body()?.getAsJsonObject("data")
                val parsed = parseDynamicMonthPanchanga(selectedYear, selectedMonthIndex, data)
                monthlyDataState = parsed
            } else {
                monthlyDataState = getFallbackMonthPanchanga(selectedYear, selectedMonthIndex)
            }
        } catch (e: Exception) {
            monthlyDataState = getFallbackMonthPanchanga(selectedYear, selectedMonthIndex)
        } finally {
            isMonthlyLoading = false
        }
    }

    val currentMonthData = monthlyDataState ?: getFallbackMonthPanchanga(selectedYear, selectedMonthIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedTab == 1) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showYearDropdown = true }
                            ) {
                                Text(
                                    text = if (isTamil) "நித்ரா நாட்காட்டி" else "Monthly Calendar",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$selectedYear",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFF9C4),
                                    fontSize = 16.sp
                                )
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Select Year",
                                    tint = Color.White,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showYearDropdown,
                                onDismissRequest = { showYearDropdown = false }
                            ) {
                                (2024..2030).forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(text = year.toString(), fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedYear = year
                                            showYearDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (isTamil) "தினசரி நாட்காட்டி" else "Astro Daily Calendar",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFB3262A))
            )
        },
        containerColor = Color(0xFFFAF7F2)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // View Switcher Tab Row (Daily vs Monthly)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFB3262A),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = if (isTamil) "தினசரி" else "Daily Calendar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (isTamil) "மாதாந்திர" else "Monthly Calendar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // ==================== DAILY CALENDAR VIEW ====================
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFB3262A).copy(alpha = 0.05f))
                            .padding(vertical = 10.dp, horizontal = 16.dp),
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
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFEEB)),
                                border = BorderStroke(3.dp, Color(0xFFB3262A)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFB3262A))
                                            .padding(vertical = 10.dp, horizontal = 16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val engMonthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
                                            Text(
                                                text = engMonthFormat.format(selectedDate.time).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp
                                            )
                                            
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
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFFF9C4))
                                            .padding(vertical = 6.dp, horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${if (isTamil) "தமிழ் வருடம்: " else "Tamil Year: "}$tamilYear",
                                            color = Color(0xFF5D4037),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = if (isTamil) "மேல் நோக்கு நாள்" else "Mel Nookku Naal",
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val dayNum = selectedDate.get(Calendar.DAY_OF_MONTH)
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 80.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFB3262A),
                                            lineHeight = 80.sp
                                        )
                                        
                                        Text(
                                            text = currentDayName.uppercase(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF2B2516),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )

                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(
                                                text = "${if (isTamil) "உதயம்: " else "Sunrise: "}$sunrise",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${if (isTamil) "அஸ்தமனம்: " else "Sunset: "}$sunset",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = Color(0xFFB3262A).copy(alpha = 0.2f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .padding(end = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF81C784), RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = if (isTamil) "நல்ல நேரம்" else "Nalla Neram",
                                                        color = Color(0xFF2E7D32),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 12.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "${if (isTamil) "காலை: " else "Morning: "}${nallaNeram.first}",
                                                        fontSize = 10.sp,
                                                        color = Color.DarkGray,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${if (isTamil) "மாலை: " else "Evening: "}${nallaNeram.second}",
                                                        fontSize = 10.sp,
                                                        color = Color.DarkGray,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            val kalams = listOf(
                                                Triple(if (isTamil) "இராகு" else "Rahu", "$rahuStart - $rahuEnd", Color(0xFFB3262A)),
                                                Triple(if (isTamil) "குளிகை" else "Gulika", "$guliStart - $guliEnd", Color(0xFFF9A825)),
                                                Triple(if (isTamil) "எமகண்டம்" else "Yemagandam", "$yamaStart - $yamaEnd", Color(0xFF2E7D32))
                                            )

                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                kalams.forEach { (label, time, color) ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = color,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                        Text(
                                                            text = time,
                                                            fontSize = 10.sp,
                                                            color = Color.DarkGray,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(150.dp)
                                                .background(Color(0xFFB3262A).copy(alpha = 0.25f))
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(0.9f)
                                                .padding(start = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val astroDetails = listOf(
                                                Pair(if (isTamil) "திதி" else "Tithi", tithiName),
                                                Pair(if (isTamil) "நட்சத்திரம்" else "Nakshatra", nakshatraName),
                                                Pair(if (isTamil) "யோகம்" else "Yoga", yogaName),
                                                Pair(if (isTamil) "கரணம்" else "Karana", karanaName),
                                                Pair(if (isTamil) "சூலம்" else "Soolam", soolamData.first),
                                                Pair(if (isTamil) "பரிகாரம்" else "Parigaram", soolamData.second)
                                            )

                                            astroDetails.forEach { (label, value) ->
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        text = label,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFB3262A)
                                                    )
                                                    Text(
                                                        text = value,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2B2516),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F5EE)),
                                border = BorderStroke(1.dp, Color(0xFFEADDBA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = if (isTamil) "இன்றைய பொன்மொழி" else "Thought of the Day",
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFB3262A),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isTamil) activeQuote.first else activeQuote.second,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        ),
                                        color = Color(0xFF5D4037)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ==================== DYNAMIC FULL INTERACTIVE MONTHLY GRID CALENDAR VIEW ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Red Month Banner (< Month - Year / Tamil Month Span >)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFA3E53)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (selectedMonthIndex > 0) {
                                    selectedMonthIndex -= 1
                                } else {
                                    selectedMonthIndex = 11
                                    selectedYear -= 1
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.ChevronLeft,
                                    contentDescription = "Previous Month",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val monthTitle = if (isTamil) "${currentMonthData.monthNameTa} - $selectedYear" else "${currentMonthData.monthNameEn} - $selectedYear"
                                val tamilSpan = if (isTamil) currentMonthData.tamilSpanTa else currentMonthData.tamilSpanEn
                                Text(
                                    text = monthTitle,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tamilSpan,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            IconButton(onClick = {
                                if (selectedMonthIndex < 11) {
                                    selectedMonthIndex += 1
                                } else {
                                    selectedMonthIndex = 0
                                    selectedYear += 1
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    if (isMonthlyLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFA3E53))
                        }
                    } else {
                        // 7-Column Days of Week Header Row (ஞா தி செ பு வி வெ ச)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val headerDays = if (isTamil) listOf("ஞா", "தி", "செ", "பு", "வி", "வெ", "ச") else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            headerDays.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1F2937),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 7-Column Dynamic Month Days Grid Matrix
                        val gridCells = remember(selectedYear, selectedMonthIndex, currentMonthData) {
                            buildDynamicCalendarGrid(selectedYear, selectedMonthIndex, currentMonthData)
                        }

                        val todayCal = Calendar.getInstance()
                        val isCurrentRealMonth = todayCal.get(Calendar.YEAR) == selectedYear && todayCal.get(Calendar.MONTH) == selectedMonthIndex
                        val realTodayDayNum = todayCal.get(Calendar.DAY_OF_MONTH)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val rowsCount = (gridCells.size + 6) / 7
                            for (rowIndex in 0 until rowsCount) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (colIndex in 0 until 7) {
                                        val cellIndex = rowIndex * 7 + colIndex
                                        if (cellIndex < gridCells.size) {
                                            val cell = gridCells[cellIndex]
                                            val isTodayBox = isCurrentRealMonth && cell.isCurrentMonth && cell.dayNumber == realTodayDayNum

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(58.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isTodayBox) {
                                                            Color(0xFF4CAF50) // Vibrant Green Box for Today!
                                                        } else if (cell.isCurrentMonth) {
                                                            Color(0xFFF9FAFB)
                                                        } else {
                                                            Color(0xFFF3F4F6)
                                                        }
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isTodayBox) Color(0xFF388E3C) else Color(0xFFE5E7EB),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable(enabled = cell.isCurrentMonth) {
                                                        val cal = Calendar.getInstance()
                                                        cal.set(selectedYear, selectedMonthIndex, cell.dayNumber)
                                                        selectedDate = cal
                                                        selectedTab = 0
                                                    }
                                                    .padding(2.dp)
                                            ) {
                                                if (cell.isCurrentMonth) {
                                                    // Top Right Corner: Tamil Date
                                                    Text(
                                                        text = "${cell.tamilDateNum}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isTodayBox) Color.White.copy(alpha = 0.9f) else Color(0xFF9CA3AF),
                                                        modifier = Modifier.align(Alignment.TopEnd)
                                                    )

                                                    // Top Left Corner: Muhurtham Icon (👑)
                                                    if (cell.isMuhurtham) {
                                                        Text(
                                                            text = "👑",
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.align(Alignment.TopStart)
                                                        )
                                                    }

                                                    // Bottom Left Corner: Amavasai (⚫) or Pournami (⚪) Badge
                                                    if (cell.isAmavasai) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.Black)
                                                                .align(Alignment.BottomStart)
                                                        )
                                                    } else if (cell.isPournami) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White)
                                                                .border(1.dp, Color.Black, CircleShape)
                                                                .align(Alignment.BottomStart)
                                                        )
                                                    }

                                                    // Center: Large Bold English Day Number
                                                    Text(
                                                        text = "${cell.dayNumber}",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isTodayBox) {
                                                            Color.White
                                                        } else if (cell.isSunday) {
                                                            Color(0xFFEF4444) // Bright Red for Sundays
                                                        } else {
                                                            Color(0xFF111827)
                                                        },
                                                        modifier = Modifier.align(Alignment.Center)
                                                    )
                                                } else {
                                                    // Trailing/Leading Days
                                                    Text(
                                                        text = "${cell.dayNumber}",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD1D5DB),
                                                        modifier = Modifier.align(Alignment.Center)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Section 1: Subamuhurtham Days Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFA3E53))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTamil) "சுபமுகூர்த்த நாட்கள்" else "Subamuhurtham Days",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    if (currentMonthData.muhurthamDays.isEmpty()) {
                                        Text(
                                            text = if (isTamil) "இந்த மாதத்தில் சுபமுகூர்த்த நாட்கள் இல்லை" else "No Subamuhurtham Days in this month",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        currentMonthData.muhurthamDays.forEachIndexed { index, item ->
                                            Text(
                                                text = if (isTamil) item.tamilText else item.englishText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF1F2937),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp)
                                            )
                                            if (index < currentMonthData.muhurthamDays.size - 1) {
                                                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 2: Fasting Days (Viradha Dinangal) Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFA3E53))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTamil) "விரத தினங்கள்" else "Fasting Days (Viradha Dinangal)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    currentMonthData.fastingList.forEachIndexed { index, fasting ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(fasting.color)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = if (isTamil) fasting.typeTa else fasting.typeEn,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1F2937),
                                                modifier = Modifier.weight(1.1f)
                                            )

                                            Text(
                                                text = if (isTamil) fasting.datesTa else fasting.datesEn,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = Color(0xFF4B5563),
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.weight(1.3f)
                                            )
                                        }

                                        if (index < currentMonthData.fastingList.size - 1) {
                                            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Parse Dynamic Backend Response
fun parseDynamicMonthPanchanga(year: Int, monthIndex: Int, data: JsonObject?): DynamicMonthData {
    val monthNamesEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val monthNamesTa = listOf("ஜனவரி", "பிப்ரவரி", "மார்ச்", "ஏப்ரல்", "மே", "ஜூன்", "ஜூலை", "ஆகஸ்ட்", "செப்டம்பர்", "அக்டோபர்", "நவம்பர்", "டிசம்பர்")

    val monthEn = monthNamesEn.getOrElse(monthIndex) { "July" }
    val monthTa = monthNamesTa.getOrElse(monthIndex) { "ஜூலை" }

    val black = Color(0xFF111827)
    val cream = Color(0xFFE6C875)
    val yellow = Color(0xFFEAB308)
    val green = Color(0xFF16A34A)
    val blue = Color(0xFF2563EB)
    val red = Color(0xFFDC2626)
    val purple = Color(0xFF9333EA)
    val orange = Color(0xFFEA580C)

    val muhurthamList = mutableListOf<MuhurthamDay>()
    val subhamuhurthamArr = data?.getAsJsonArray("subhamuhurtham")
    subhamuhurthamArr?.forEach { elem ->
        val obj = elem.asJsonObject
        val dayNum = obj.get("day")?.asInt ?: 1
        val tamilStr = obj.get("tamil")?.asString ?: ""
        val englishStr = obj.get("english")?.asString ?: ""
        muhurthamList.add(MuhurthamDay(dayNum, englishStr, tamilStr))
    }

    val fastingObj = data?.getAsJsonObject("fasting")

    val amavasaiDays = mutableSetOf<Int>()
    val pournamiDays = mutableSetOf<Int>()

    val fastingItems = mutableListOf<FastingItem>()

    fun parseFastingGroup(typeEn: String, typeTa: String, jsonArr: JsonArray?, color: Color, isAmavasai: Boolean = false, isPournami: Boolean = false) {
        if (jsonArr == null || jsonArr.size() == 0) {
            fastingItems.add(FastingItem(typeEn, typeTa, "-", "-", color))
            return
        }
        val enDates = mutableListOf<String>()
        val taDates = mutableListOf<String>()
        jsonArr.forEach { elem ->
            val obj = elem.asJsonObject
            val dayNum = obj.get("day")?.asInt
            if (dayNum != null) {
                if (isAmavasai) amavasaiDays.add(dayNum)
                if (isPournami) pournamiDays.add(dayNum)
            }
            obj.get("english")?.asString?.let { enDates.add(it) }
            obj.get("tamil")?.asString?.let { taDates.add(it) }
        }
        fastingItems.add(FastingItem(typeEn, typeTa, enDates.joinToString(", "), taDates.joinToString(", "), color))
    }

    parseFastingGroup("Amavasai", "அமாவாசை", fastingObj?.getAsJsonArray("amavasai"), black, isAmavasai = true)
    parseFastingGroup("Pournami", "பௌர்ணமி", fastingObj?.getAsJsonArray("pournami"), cream, isPournami = true)
    parseFastingGroup("Krittika", "கார்த்திகை", fastingObj?.getAsJsonArray("kiruthigai"), yellow)
    parseFastingGroup("Shravana (Thiruvonam)", "திருவோணம்", fastingObj?.getAsJsonArray("thiruvonam"), green)
    parseFastingGroup("Ekadashi", "ஏகாதசி", fastingObj?.getAsJsonArray("ekadashi"), blue)
    parseFastingGroup("Sashti", "சஷ்டி", fastingObj?.getAsJsonArray("sashti"), red)
    parseFastingGroup("Pradosham", "பிரதோஷம்", fastingObj?.getAsJsonArray("pradosham"), purple)
    parseFastingGroup("Chaturthi", "சதுர்த்தி", fastingObj?.getAsJsonArray("chaturthi"), orange)

    val tamilSpans = mapOf(
        0 to Pair("Margazhi - Thai", "மார்கழி - தை"),
        1 to Pair("Thai - Maasi", "தை - மாசி"),
        2 to Pair("Maasi - Panguni", "மாசி - பங்குனி"),
        3 to Pair("Panguni - Chithirai", "பங்குனி - சித்திரை"),
        4 to Pair("Chithirai - Vaikasi", "சித்திரை - வைகாசி"),
        5 to Pair("Vaikasi - Aani", "வைகாசி - ஆனி"),
        6 to Pair("Aani - Aadi", "ஆனி - ஆடி"),
        7 to Pair("Aadi - Avani", "ஆடி - ஆவணி"),
        8 to Pair("Avani - Purattasi", "ஆவணி - புரட்டாசி"),
        9 to Pair("Purattasi - Aippasi", "புரட்டாசி - ஐப்பசி"),
        10 to Pair("Aippasi - Karthikai", "ஐப்பசி - கார்த்திகை"),
        11 to Pair("Karthikai - Margazhi", "கார்த்திகை - மார்கழி")
    )
    val span = tamilSpans[monthIndex] ?: Pair("Aani - Aadi", "ஆனி - ஆடி")

    return DynamicMonthData(
        year = year,
        monthIndex = monthIndex,
        monthNameEn = monthEn,
        monthNameTa = monthTa,
        tamilSpanEn = span.first,
        tamilSpanTa = span.second,
        muhurthamDays = muhurthamList,
        fastingList = fastingItems,
        amavasaiDays = amavasaiDays,
        pournamiDays = pournamiDays
    )
}

// Fallback Month Panchanga
fun getFallbackMonthPanchanga(year: Int, monthIndex: Int): DynamicMonthData {
    return parseDynamicMonthPanchanga(year, monthIndex, null)
}

// Build 7-Column Calendar Grid Cells for Dynamic Month View
fun buildDynamicCalendarGrid(year: Int, monthIndex: Int, monthData: DynamicMonthData): List<CalendarGridCell> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, monthIndex)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7
    val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val prevCal = cal.clone() as Calendar
    prevCal.add(Calendar.MONTH, -1)
    val maxDaysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = mutableListOf<CalendarGridCell>()

    for (i in (firstDayOfWeek - 1) downTo 0) {
        val prevDayNum = maxDaysInPrevMonth - i
        cells.add(CalendarGridCell(prevDayNum, false, 0, false, false, false, false))
    }

    val muhurthamDaysSet = monthData.muhurthamDays.map { it.dayNum }.toSet()

    val tamilOffsets = mapOf(
        0 to Pair(17, 15), 1 to Pair(18, 14), 2 to Pair(17, 15), 3 to Pair(18, 14),
        4 to Pair(18, 15), 5 to Pair(18, 15), 6 to Pair(17, 17), 7 to Pair(16, 17),
        8 to Pair(16, 17), 9 to Pair(15, 17), 10 to Pair(16, 16), 11 to Pair(16, 16)
    )
    val offset = tamilOffsets[monthIndex] ?: Pair(17, 17)

    for (day in 1..maxDaysInMonth) {
        val currentDayCal = cal.clone() as Calendar
        currentDayCal.set(Calendar.DAY_OF_MONTH, day)
        val dayOfWeekIndex = (currentDayCal.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7
        val isSunday = dayOfWeekIndex == 0

        val tamilDateNum = if (day < offset.second) {
            offset.first + (day - 1)
        } else {
            1 + (day - offset.second)
        }

        val isMuhurtham = muhurthamDaysSet.contains(day)
        val isAmavasai = monthData.amavasaiDays.contains(day)
        val isPournami = monthData.pournamiDays.contains(day)

        cells.add(
            CalendarGridCell(
                dayNumber = day,
                isCurrentMonth = true,
                tamilDateNum = tamilDateNum,
                isSunday = isSunday,
                isMuhurtham = isMuhurtham,
                isAmavasai = isAmavasai,
                isPournami = isPournami
            )
        )
    }

    val remainingCells = 7 - (cells.size % 7)
    if (remainingCells < 7) {
        for (nextDay in 1..remainingCells) {
            cells.add(CalendarGridCell(nextDay, false, 0, false, false, false, false))
        }
    }

    return cells
}
