package com.astroeleven.app.ui.horoscope

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class FreeHoroscopeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                FreeHoroscopeScreen(
                    onBackClick = { finish() },
                    onGenerateChart = { data -> launchChart(data) }
                )
            }
        }
    }

    private fun launchChart(data: BirthData) {
        val payload = JsonObject().apply {
            addProperty("name", data.name)
            addProperty("day", data.day)
            addProperty("month", data.month)
            addProperty("year", data.year)
            addProperty("hour", data.hour)
            addProperty("minute", data.minute)
            addProperty("gender", data.gender)
            addProperty("country", data.country)
            addProperty("state", data.state)
            addProperty("city", data.city)
            addProperty("timezone", data.timezone)
            addProperty("latitude", data.latitude)
            addProperty("longitude", data.longitude)
        }

        val intent = Intent(this, com.astroeleven.app.ui.chart.VipChartActivity::class.java).apply {
            putExtra("birthData", payload.toString())
        }
        startActivity(intent)
    }
}

data class BirthData(
    val name: String,
    val day: Int,
    val month: Int,
    val year: Int,
    val hour: Int,
    val minute: Int,
    val gender: String,
    val country: String,
    val state: String,
    val city: String,
    val timezone: Double,
    val latitude: Double,
    val longitude: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeHoroscopeScreen(
    onBackClick: () -> Unit,
    onGenerateChart: (BirthData) -> Unit
) {
    val context = LocalContext.current

    // Form State
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") } // Default female matching mockup

    // Date
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    // Time
    var hour by remember { mutableStateOf("") }
    var minute by remember { mutableStateOf("") }
    var amPm by remember { mutableStateOf("AM") }

    // Place
    var countryName by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("") }
    var cityName by remember { mutableStateOf("") }
    var timezoneId by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var timezone by remember { mutableStateOf<Double?>(null) }

    // Location Picker
    val placeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val d = result.data!!
            val fullName = d.getStringExtra("name") ?: ""
            val cityRes = d.getStringExtra("city") ?: ""
            val stateRes = d.getStringExtra("state") ?: ""
            val countryRes = d.getStringExtra("country") ?: ""
            val tzId = d.getStringExtra("timezoneId")
            val latRes = d.getDoubleExtra("lat", 0.0)
            val lonRes = d.getDoubleExtra("lon", 0.0)

            cityName = if (cityRes.isNotBlank()) cityRes else fullName
            stateName = stateRes
            countryName = countryRes
            timezoneId = tzId?.takeIf { it.isNotBlank() }
            latitude = latRes.takeIf { it != 0.0 }
            longitude = lonRes.takeIf { it != 0.0 }

            val computed = computeTimezoneOffsetHours(timezoneId, day, month, year, hour, minute)
            if (computed != null) timezone = computed
        }
    }

    val computedTimezone = remember(timezoneId, day, month, year, hour, minute) {
        computeTimezoneOffsetHours(timezoneId, day, month, year, hour, minute)
    }
    val timezoneOffset = computedTimezone ?: timezone
    val timezoneDisplay = timezoneOffset?.let { formatUtcOffset(it) } ?: ""

    val showDatePicker = {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, com.astroeleven.app.R.style.DialogPickerTheme, { _, py, pm, pd ->
            year = py.toString(); month = String.format("%02d", pm + 1); day = String.format("%02d", pd)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val showTimePicker = {
        TimePickerDialog(context, com.astroeleven.app.R.style.DialogPickerTheme, { _, ph, pm ->
            val hTyped = if (ph > 12) (ph - 12) else if (ph == 0) 12 else ph
            hour = String.format("%02d", hTyped); minute = String.format("%02d", pm); amPm = if (ph >= 12) "PM" else "AM"
        }, 12, 0, false).show()
    }

    val launchLocationPicker = {
        val intent = Intent(context, com.astroeleven.app.ui.city.CitySearchActivity::class.java)
        placeLauncher.launch(intent)
    }

    var isLoading by remember { mutableStateOf(false) }

    // Premium Color Palette
    val royalOrange = Color(0xFFFF8C00)
    val goldenYellow = Color(0xFFFFC107)
    val softCream = Color(0xFFFFF9F0)
    val lightGold = Color(0xFFF8E8C2)
    val textDark = Color(0xFF3E2723)
    val textGrey = Color(0xFF7D6F5C)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(softCream, Color.White)
                )
            )
    ) {
        // Subtle Background Zodiac patterns & Sparkling Stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background stars
            val starPositions = listOf(
                Offset(size.width * 0.1f, size.height * 0.2f),
                Offset(size.width * 0.85f, size.height * 0.15f),
                Offset(size.width * 0.2f, size.height * 0.45f),
                Offset(size.width * 0.9f, size.height * 0.55f),
                Offset(size.width * 0.15f, size.height * 0.8f),
                Offset(size.width * 0.75f, size.height * 0.85f)
            )
            starPositions.forEach { pos ->
                drawCircle(color = goldenYellow.copy(alpha = 0.4f), radius = 3.dp.toPx(), center = pos)
                drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = pos)
            }
        }

        // Top Right Zodiac Wheel Illustration
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
        ) {
            Canvas(modifier = Modifier.size(190.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2
                // Outer ring
                drawCircle(color = lightGold.copy(alpha = 0.5f), radius = radius, style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = lightGold.copy(alpha = 0.3f), radius = radius - 12.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = lightGold.copy(alpha = 0.2f), radius = radius - 30.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
                // Radiating divisions
                for (i in 0 until 12) {
                    val angle = (i * 30) * (Math.PI / 180)
                    val startX = center.x + (radius - 30.dp.toPx()) * cos(angle).toFloat()
                    val startY = center.y + (radius - 30.dp.toPx()) * sin(angle).toFloat()
                    val endX = center.x + radius * cos(angle).toFloat()
                    val endY = center.y + radius * sin(angle).toFloat()
                    drawLine(color = lightGold.copy(alpha = 0.4f), start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 1.dp.toPx())
                }
                // Center Glowing Sun
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(goldenYellow, royalOrange, Color.Transparent),
                        center = center,
                        radius = 24.dp.toPx()
                    ),
                    radius = 24.dp.toPx()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button Square rounded
                Card(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, lightGold),
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = royalOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Free Horoscope",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textDark
                    )
                    Text(
                        text = "Create your birth chart and unlock your destiny ✨",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Glassmorphism Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(royalOrange.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = "User details",
                                tint = royalOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Personal Details",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                            Text(
                                text = "Please enter your details accurately",
                                fontSize = 12.sp,
                                color = textGrey
                            )
                        }
                    }

                    // Decorative Orange indicator line
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(royalOrange)
                    )

                    // Text Field Design System
                    val inputColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textDark,
                        unfocusedTextColor = textDark,
                        focusedBorderColor = royalOrange,
                        unfocusedBorderColor = lightGold,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = royalOrange
                    )

                    // Full Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Full Name", color = textGrey, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = royalOrange)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = inputColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
                        )
                    )

                    // Gender Segmented Picker
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Gender",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val genders = listOf("Male", "Female", "Other")
                            genders.forEach { item ->
                                val isSelected = gender == item
                                val buttonBg = if (isSelected) {
                                    Brush.horizontalGradient(colors = listOf(goldenYellow, royalOrange))
                                } else {
                                    Brush.linearGradient(colors = listOf(Color.White, Color.White))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(buttonBg)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.Transparent else lightGold,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { gender = item },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        color = if (isSelected) Color.White else textGrey,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Date of Birth - 3 Columns selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Date of Birth",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // DD box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .border(1.dp, lightGold, RoundedCornerShape(18.dp))
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .clickable { showDatePicker() }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = royalOrange, modifier = Modifier.size(16.dp))
                                    Text(text = day.ifBlank { "DD" }, color = if (day.isNotBlank()) textDark else textGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = textGrey, modifier = Modifier.size(16.dp))
                                }
                            }

                            // MM box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .border(1.dp, lightGold, RoundedCornerShape(18.dp))
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .clickable { showDatePicker() }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = royalOrange, modifier = Modifier.size(16.dp))
                                    Text(text = month.ifBlank { "MM" }, color = if (month.isNotBlank()) textDark else textGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = textGrey, modifier = Modifier.size(16.dp))
                                }
                            }

                            // YYYY box
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(58.dp)
                                    .border(1.dp, lightGold, RoundedCornerShape(18.dp))
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .clickable { showDatePicker() }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = royalOrange, modifier = Modifier.size(16.dp))
                                    Text(text = year.ifBlank { "YYYY" }, color = if (year.isNotBlank()) textDark else textGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = textGrey, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Time of Birth - 3 Columns selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Time of Birth",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // HH box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .border(1.dp, lightGold, RoundedCornerShape(18.dp))
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .clickable { showTimePicker() }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = royalOrange, modifier = Modifier.size(16.dp))
                                    Text(text = hour.ifBlank { "HH" }, color = if (hour.isNotBlank()) textDark else textGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = textGrey, modifier = Modifier.size(16.dp))
                                }
                            }

                            // MM box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .border(1.dp, lightGold, RoundedCornerShape(18.dp))
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .clickable { showTimePicker() }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = royalOrange, modifier = Modifier.size(16.dp))
                                    Text(text = minute.ifBlank { "MM" }, color = if (minute.isNotBlank()) textDark else textGrey, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = textGrey, modifier = Modifier.size(16.dp))
                                }
                            }

                            // AM/PM box
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(58.dp)
                                    .border(1.dp, lightGold, RoundedCornerShape(18.dp))
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .clickable { amPm = if (amPm == "AM") "PM" else "AM" }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = royalOrange, modifier = Modifier.size(16.dp))
                                    Text(text = amPm, color = textDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = textGrey, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Place of Birth
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Place of Birth",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clickable { launchLocationPicker() }
                                .border(1.dp, lightGold, RoundedCornerShape(18.dp))
                                .background(Color.White, RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Place,
                                        contentDescription = null,
                                        tint = royalOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = cityName.ifBlank { "City of Birth" },
                                        color = if (cityName.isNotBlank()) textDark else textGrey,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = textGrey)
                            }
                        }
                        
                        if (timezoneDisplay.isNotBlank()) {
                            Text(
                                text = "Timezone: $timezoneDisplay",
                                fontSize = 11.sp,
                                color = textGrey,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Generate Rasi Chart Button (Glossy orange gradient)
                    Button(
                        onClick = {
                            if (validateInputs(name, day, month, year, hour, minute, cityName, timezoneOffset)) {
                                isLoading = true
                                val h = hour.toIntOrNull() ?: 0
                                val hour24 = if (amPm == "PM" && h < 12) h + 12
                                            else if (amPm == "AM" && h == 12) 0
                                            else h

                                onGenerateChart(BirthData(
                                    name = name,
                                    day = day.toIntOrNull() ?: 0,
                                    month = month.toIntOrNull() ?: 0,
                                    year = year.toIntOrNull() ?: 0,
                                    hour = hour24,
                                    minute = minute.toIntOrNull() ?: 0,
                                    gender = gender,
                                    country = countryName,
                                    state = stateName,
                                    city = cityName,
                                    timezone = timezoneOffset ?: 5.5,
                                    latitude = latitude ?: 0.0,
                                    longitude = longitude ?: 0.0
                                ))
                            } else {
                                Toast.makeText(context, "Please fill all details", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(goldenYellow, royalOrange)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "✨ GENERATE RASI CHART",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Badges Section (Horizontally aligned, wrapping text aligned to center)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BadgeItem(
                    title = "100% Secure",
                    desc = "Your Data is Safe",
                    icon = Icons.Rounded.VerifiedUser,
                    color = royalOrange
                )
                BadgeItem(
                    title = "Instant Results",
                    desc = "Get it in Seconds",
                    icon = Icons.Rounded.ElectricBolt,
                    color = royalOrange
                )
                BadgeItem(
                    title = "Vedic Astrology",
                    desc = "Accurate Prediction",
                    icon = Icons.Rounded.AutoAwesome,
                    color = royalOrange
                )
                BadgeItem(
                    title = "Millions Trusted",
                    desc = "Happy Customers",
                    icon = Icons.Rounded.People,
                    color = royalOrange
                )
            }
        }
    }
}

@Composable
fun RowScope.BadgeItem(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3E2723),
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            fontSize = 9.sp,
            color = Color(0xFF7D6F5C),
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 11.sp
        )
    }
}

private fun validateInputs(
    name: String,
    day: String,
    month: String,
    year: String,
    hour: String,
    minute: String,
    city: String,
    timezone: Double?
): Boolean {
    return name.isNotBlank() &&
            day.isNotBlank() &&
            month.isNotBlank() &&
            year.isNotBlank() &&
            hour.isNotBlank() &&
            minute.isNotBlank() &&
            city.isNotBlank() &&
            timezone != null
}

private fun computeTimezoneOffsetHours(
    timezoneId: String?,
    day: String,
    month: String,
    year: String,
    hour: String,
    minute: String
): Double? {
    if (timezoneId.isNullOrBlank()) return null
    val tz = TimeZone.getTimeZone(timezoneId)

    val dayInt = day.toIntOrNull()
    val monthInt = month.toIntOrNull()
    val yearInt = year.toIntOrNull()
    val hourInt = hour.toIntOrNull() ?: 0
    val minuteInt = minute.toIntOrNull() ?: 0

    val offsetMillis = if (dayInt != null && monthInt != null && yearInt != null) {
        val cal = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, yearInt)
            set(Calendar.MONTH, (monthInt - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, dayInt.coerceIn(1, 31))
            set(Calendar.HOUR_OF_DAY, hourInt.coerceIn(0, 23))
            set(Calendar.MINUTE, minuteInt.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        tz.getOffset(cal.timeInMillis)
    } else {
        tz.rawOffset
    }

    return offsetMillis / 3600000.0
}

private fun formatUtcOffset(offsetHours: Double): String {
    val totalMinutes = (offsetHours * 60).roundToInt()
    val sign = if (totalMinutes >= 0) "+" else "-"
    val absMinutes = abs(totalMinutes)
    val hours = absMinutes / 60
    val minutes = absMinutes % 60
    return "UTC$sign${"%02d".format(hours)}:${"%02d".format(minutes)}"
}
