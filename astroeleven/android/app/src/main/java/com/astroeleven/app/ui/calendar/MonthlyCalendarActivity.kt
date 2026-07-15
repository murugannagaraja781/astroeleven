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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astroeleven.app.data.api.ApiClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.*

class MonthlyCalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isTamil = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("is_tamil", false)

        setContent {
            MaterialTheme {
                MonthlyCalendarScreen(isTamil = isTamil, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCalendarScreen(isTamil: Boolean, onBack: () -> Unit) {
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) } // 1-indexed
    var showYearDropdown by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Lists states
    var muhurthamList by remember { mutableStateOf<List<String>>(emptyList()) }
    var amavasaiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var pournamiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var kiruthigaiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var thiruvonamList by remember { mutableStateOf<List<String>>(emptyList()) }
    var ekadashiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var sashtiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var pradoshamList by remember { mutableStateOf<List<String>>(emptyList()) }
    var chaturthiList by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Special days states
    var ashtamiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var navamiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var dasamiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var kariDaysList by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Festivals
    var hinduFestivals by remember { mutableStateOf<List<String>>(emptyList()) }
    var muslimFestivals by remember { mutableStateOf<List<String>>(emptyList()) }
    var christianFestivals by remember { mutableStateOf<List<String>>(emptyList()) }

    val monthNamesTamil = listOf(
        "", "ஜனவரி", "பிப்ரவரி", "மார்ச்", "ஏப்ரல்", "மே", "ஜூன்", 
        "ஜூலை", "ஆகஸ்ட்", "செப்டம்பர்", "அக்டோபர்", "நவம்பர்", "டிசம்பர்"
    )
    val monthNamesEnglish = listOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val currentMonthName = if (isTamil) monthNamesTamil[selectedMonth] else monthNamesEnglish[selectedMonth]

    LaunchedEffect(selectedYear, selectedMonth) {
        isLoading = true
        errorMessage = null
        try {
            val body = JsonObject().apply {
                addProperty("year", selectedYear)
                addProperty("month", selectedMonth)
                addProperty("lat", 13.0827)
                addProperty("lng", 80.2707)
            }
            val resp = ApiClient.api.getMonthlyPanchanga(body)
            if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) {
                val data = resp.body()?.getAsJsonObject("data")
                
                // Helper to extract string lists
                val extractList = { arr: JsonArray?, langKey: String ->
                    val list = mutableListOf<String>()
                    arr?.forEach { element ->
                        val item = element.asJsonObject
                        list.add(item.get(langKey).asString)
                    }
                    list
                }

                // Extracted muhurtham
                val muhArray = data?.getAsJsonArray("subhamuhurtham")
                muhurthamList = extractList(muhArray, if (isTamil) "tamil" else "english")

                // Extracted fasting
                val fast = data?.getAsJsonObject("fasting")
                amavasaiList = extractList(fast?.getAsJsonArray("amavasai"), if (isTamil) "tamil" else "english")
                pournamiList = extractList(fast?.getAsJsonArray("pournami"), if (isTamil) "tamil" else "english")
                kiruthigaiList = extractList(fast?.getAsJsonArray("kiruthigai"), if (isTamil) "tamil" else "english")
                thiruvonamList = extractList(fast?.getAsJsonArray("thiruvonam"), if (isTamil) "tamil" else "english")
                ekadashiList = extractList(fast?.getAsJsonArray("ekadashi"), if (isTamil) "tamil" else "english")
                sashtiList = extractList(fast?.getAsJsonArray("sashti"), if (isTamil) "tamil" else "english")
                pradoshamList = extractList(fast?.getAsJsonArray("pradosham"), if (isTamil) "tamil" else "english")
                chaturthiList = extractList(fast?.getAsJsonArray("chaturthi"), if (isTamil) "tamil" else "english")

                // Extracted other days
                val other = data?.getAsJsonObject("otherDays")
                ashtamiList = extractList(other?.getAsJsonArray("ashtami"), if (isTamil) "tamil" else "english")
                navamiList = extractList(other?.getAsJsonArray("navami"), if (isTamil) "tamil" else "english")
                dasamiList = extractList(other?.getAsJsonArray("dasami"), if (isTamil) "tamil" else "english")
                kariDaysList = extractList(other?.getAsJsonArray("kariDays"), if (isTamil) "tamil" else "english")

                // Extracted festivals
                val fests = data?.getAsJsonObject("festivals")
                val extractFest = { arr: JsonArray? ->
                    val list = mutableListOf<String>()
                    arr?.forEach { element ->
                        val item = element.asJsonObject
                        list.add("${item.get("day").asInt} - ${item.get("name").asString}")
                    }
                    list
                }
                hinduFestivals = extractFest(fests?.getAsJsonArray("hindu"))
                muslimFestivals = extractFest(fests?.getAsJsonArray("muslim"))
                christianFestivals = extractFest(fests?.getAsJsonArray("christian"))
            } else {
                errorMessage = "Failed to load monthly data"
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Connection Error"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showYearDropdown = true }
                    ) {
                        Text(
                            text = if (isTamil) "மாதாந்திர காலண்டர்" else "Monthly Calendar",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedYear.toString(),
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp
                        )
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = "Select Year",
                            tint = Color.White
                        )
                        
                        DropdownMenu(
                            expanded = showYearDropdown,
                            onDismissRequest = { showYearDropdown = false }
                        ) {
                            listOf(2025, 2026, 2027, 2028).forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        selectedYear = year
                                        showYearDropdown = false
                                    }
                                )
                            }
                        }
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
        containerColor = Color(0xFFFFFDF9)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month Selector Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB3262A).copy(alpha = 0.05f))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedMonth > 1) {
                        selectedMonth--
                    } else {
                        selectedMonth = 12
                        selectedYear--
                    }
                }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Prev Month", tint = Color(0xFFB3262A), modifier = Modifier.size(32.dp))
                }

                Text(
                    text = currentMonthName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFB3262A)
                )

                IconButton(onClick = {
                    if (selectedMonth < 12) {
                        selectedMonth++
                    } else {
                        selectedMonth = 1
                        selectedYear++
                    }
                }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Month", tint = Color(0xFFB3262A), modifier = Modifier.size(32.dp))
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
                            onClick = { selectedYear = selectedYear }, // Trigger reload
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
                    // 1. Subhamuhurtha Days Section
                    MonthlyCategoryCard(
                        title = if (isTamil) "சுபமுகூர்த்த தினங்கள்" else "Subamuhurtham Days",
                        headerColor = Color(0xFFE1353C)
                    ) {
                        if (muhurthamList.isEmpty()) {
                            Text(
                                text = if (isTamil) "இந்த மாதத்தில் முகூர்த்த நாட்கள் இல்லை." else "No Muhurtham days in this month.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            Column(modifier = Modifier.padding(8.dp)) {
                                muhurthamList.forEach { day ->
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 12.dp)
                                    )
                                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }

                    // 2. Fasting Days (Viradha Dinangal) Section
                    MonthlyCategoryCard(
                        title = if (isTamil) "விரத தினங்கள்" else "Fasting Days (Viradha Dinangal)",
                        headerColor = Color(0xFFE1353C)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FastingRow(title = if (isTamil) "அமாவாசை" else "Amavasai", days = amavasaiList, iconColor = Color.Black)
                            FastingRow(title = if (isTamil) "பௌர்ணமி" else "Pournami", days = pournamiList, iconColor = Color(0xFFEADDBA))
                            FastingRow(title = if (isTamil) "கிருத்திகை" else "Krittika", days = kiruthigaiList, iconColor = Color(0xFFFDBA16))
                            FastingRow(title = if (isTamil) "திருவோணம்" else "Shravana (Thiruvonam)", days = thiruvonamList, iconColor = Color(0xFF2E7D32))
                            FastingRow(title = if (isTamil) "ஏகாதசி" else "Ekadashi", days = ekadashiList, iconColor = Color(0xFF1E88E5))
                            FastingRow(title = if (isTamil) "சஷ்டி" else "Sashti", days = sashtiList, iconColor = Color(0xFFE1353C))
                            FastingRow(title = if (isTamil) "பிரதோஷம்" else "Pradosham", days = pradoshamList, iconColor = Color(0xFF8E24AA))
                            FastingRow(title = if (isTamil) "சதுர்த்தி" else "Chaturthi", days = chaturthiList, iconColor = Color(0xFFF4511E))
                        }
                    }

                    // 3. Other Days (Ashtami, Navami, Dasami, Kari)
                    MonthlyCategoryCard(
                        title = if (isTamil) "மற்ற தினங்கள்" else "Special Days",
                        headerColor = Color(0xFFE1353C)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SpecialDayGridCell(title = if (isTamil) "அஷ்டமி" else "Ashtami", days = ashtamiList, modifier = Modifier.weight(1f))
                                VerticalDivider(modifier = Modifier.height(60.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.2f))
                                SpecialDayGridCell(title = if (isTamil) "நவமி" else "Navami", days = navamiList, modifier = Modifier.weight(1f))
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SpecialDayGridCell(title = if (isTamil) "தசமி" else "Dasami", days = dasamiList, modifier = Modifier.weight(1f))
                                VerticalDivider(modifier = Modifier.height(60.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.2f))
                                SpecialDayGridCell(title = if (isTamil) "கரி நாட்கள்" else "Kari Days", days = kariDaysList, modifier = Modifier.weight(1f), isAlert = true)
                            }
                        }
                    }

                    // 4. Hindu Festivals
                    MonthlyFestivalSection(title = if (isTamil) "இந்து பண்டிகைகள்" else "Hindu Festivals", festivals = hinduFestivals)

                    // 5. Muslim Festivals
                    MonthlyFestivalSection(title = if (isTamil) "முஸ்லீம் பண்டிகைகள்" else "Muslim Festivals", festivals = muslimFestivals)

                    // 6. Christian Festivals
                    MonthlyFestivalSection(title = if (isTamil) "கிறிஸ்தவ பண்டிகைகள்" else "Christian Festivals", festivals = christianFestivals)
                }
            }
        }
    }
}

@Composable
fun MonthlyCategoryCard(
    title: String,
    headerColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEADDBA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(vertical = 10.dp, horizontal = 14.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            content()
        }
    }
}

@Composable
fun FastingRow(
    title: String,
    days: List<String>,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(iconColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = if (days.isEmpty()) "-" else days.joinToString(", "),
            fontSize = 14.sp,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SpecialDayGridCell(
    title: String,
    days: List<String>,
    modifier: Modifier,
    isAlert: Boolean = false
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isAlert) Color(0xFFB3262A) else Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (days.isEmpty()) "-" else days.joinToString(", "),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = Color.DarkGray
        )
    }
}

@Composable
fun MonthlyFestivalSection(
    title: String,
    festivals: List<String>
) {
    if (festivals.isNotEmpty()) {
        MonthlyCategoryCard(title = title, headerColor = Color(0xFFE1353C)) {
            Column(modifier = Modifier.padding(10.dp)) {
                festivals.forEach { fest ->
                    Text(
                        text = fest,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                    )
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                }
            }
        }
    }
}
