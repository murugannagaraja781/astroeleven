package com.astroeleven.app.ui.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astroeleven.app.data.api.ApiClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.*

class MuhurthamActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isTamil = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("is_tamil", false)

        setContent {
            MaterialTheme {
                MuhurthamScreen(isTamil = isTamil, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuhurthamScreen(isTamil: Boolean, onBack: () -> Unit) {
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) } // 1-indexed
    var showYearDropdown by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var muhurthamList by remember { mutableStateOf<List<String>>(emptyList()) }

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
                val muhArray = data?.getAsJsonArray("subhamuhurtham")
                
                val list = mutableListOf<String>()
                muhArray?.forEach { element ->
                    list.add(element.asJsonObject.get(if (isTamil) "tamil" else "english").asString)
                }
                muhurthamList = list
            } else {
                errorMessage = "Failed to load Muhurtham calculation"
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
                            text = if (isTamil) "சுபமுகூர்த்த நாட்கள்" else "Subamuhurtham Days",
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
            // Month Switcher Header
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
                            onClick = { selectedYear = selectedYear },
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
                                    .background(Color(0xFFE1353C))
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isTamil) "சுபமுகூர்த்த தேதிகள் பட்டியல்" else "Subamuhurtham Dates List",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            if (muhurthamList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTamil) "இந்த மாதத்தில் சுபமுகூர்த்த நாட்கள் எதுவும் இல்லை." else "No Subamuhurtham days in this month.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    muhurthamList.forEachIndexed { index, date ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFFB3262A), RoundedCornerShape(4.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = date,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                ),
                                                color = Color(0xFF2B2516),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (index < muhurthamList.size - 1) {
                                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Disclaimer details Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF0)),
                        border = BorderStroke(1.dp, Color(0xFFEADDBA).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isTamil) "குறிப்பு:" else "Note:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB3262A),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isTamil)
                                    "மேலே கொடுக்கப்பட்டுள்ள நாட்கள் அனைத்தும் பஞ்சாங்க கணிதத்தின் படி கணிக்கப்பட்ட சுபமுகூர்த்த நாட்கள் ஆகும்."
                                else
                                    "All dates listed above are auspicious wedding/muhurtham days calculated dynamically using astronomical calculations.",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
