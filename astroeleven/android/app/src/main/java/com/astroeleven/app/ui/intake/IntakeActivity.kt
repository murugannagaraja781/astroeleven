package com.astroeleven.app.ui.intake

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import com.astroeleven.app.ui.theme.*
import com.astroeleven.app.utils.Localization
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.astroeleven.app.data.local.TokenManager
import com.astroeleven.app.data.remote.SocketManager
import com.astroeleven.app.ui.chat.ChatActivity
import com.astroeleven.app.ui.theme.CosmicAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

class IntakeActivity : ComponentActivity() {

    private var partnerId: String? = null
    private var type: String? = null
    private var partnerName: String? = null
    private var partnerImage: String? = null
    private var isEditMode = false
    private var existingData: JSONObject? = null
    private var targetUserId: String? = null

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)

        partnerId = intent.getStringExtra("partnerId")
        type = intent.getStringExtra("type")
        partnerName = intent.getStringExtra("partnerName") ?: "Astrologer"
        partnerImage = intent.getStringExtra("partnerImage")
        isEditMode = intent.getBooleanExtra("isEditMode", false)
        targetUserId = intent.getStringExtra("targetUserId")
        val isMatchingFromIntent = intent.getBooleanExtra("isMatching", false)
        android.util.Log.d("IntakeActivity", "Init: isMatching=$isMatchingFromIntent")

        val dataStr = intent.getStringExtra("existingData")
        if (dataStr != null) {
            try { existingData = JSONObject(dataStr) } catch(e: Exception){}
        }

        setContent {
            CosmicAppTheme {
                IntakeScreen(
                    partnerId = partnerId,
                    partnerName = partnerName!!,
                    partnerImage = partnerImage,
                    callType = type,
                    isEditMode = isEditMode,
                    isMatching = isMatchingFromIntent,
                    existingData = existingData,
                    targetUserId = targetUserId,
                    tokenManager = tokenManager,
                    onClose = { finish() },
                    onSessionConnected = { sessionId, callType ->
                        navigateToSession(sessionId, callType)
                    },
                    onUnanswered = {
                        Toast.makeText(this, "No response from astrologer", Toast.LENGTH_LONG).show()
                        finish()
                    },
                    saveForm = { saveFormData(it) },
                    loadForm = { loadSavedFormData() }
                )
            }
        }
    }

    private fun navigateToSession(sessionId: String, type: String) {
        val savedData = loadSavedFormData()?.toString()
        if (type == "chat") {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("sessionId", sessionId)
                putExtra("toUserId", partnerId)
                putExtra("toUserName", partnerName)
                if (savedData != null) {
                    putExtra("birthData", savedData)
                }
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, com.astroeleven.app.ui.call.CallActivity::class.java).apply {
                putExtra("sessionId", sessionId)
                putExtra("partnerId", partnerId)
                putExtra("partnerName", partnerName)
                putExtra("isInitiator", true)
                putExtra("callType", type)
                if (savedData != null) {
                    putExtra("birthData", savedData)
                }
            }
            startActivity(intent)
        }
        finish()
    }

    private fun saveFormData(data: JSONObject) {
        try {
            val prefs = getSharedPreferences("Astro ElevenIntake", Context.MODE_PRIVATE)
            prefs.edit().putString("lastForm", data.toString()).apply()
        } catch (e: Exception) {}
    }

    private fun loadSavedFormData(): JSONObject? {
        return try {
            val prefs = getSharedPreferences("Astro ElevenIntake", Context.MODE_PRIVATE)
            val json = prefs.getString("lastForm", null)
            if (json != null) JSONObject(json) else null
        } catch (e: Exception) { null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    partnerId: String?,
    partnerName: String,
    partnerImage: String?,
    callType: String?,
    isEditMode: Boolean,
    isMatching: Boolean,
    existingData: JSONObject?,
    targetUserId: String?,
    tokenManager: TokenManager,
    onClose: () -> Unit,
    onSessionConnected: (String, String) -> Unit,
    onUnanswered: () -> Unit,
    saveForm: (JSONObject) -> Unit,
    loadForm: () -> JSONObject?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Form State
    var isTamil by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }

    // Date
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    // Time
    var hour by remember { mutableStateOf("") }
    var minute by remember { mutableStateOf("") }
    var amPm by remember { mutableStateOf("AM") }

    // Place
    var cityName by remember { mutableStateOf("") }
    var timezoneId by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var timezone by remember { mutableStateOf<Double?>(null) }

    // Partner State
    var pName by remember { mutableStateOf("") }
    var pGender by remember { mutableStateOf("Female") }
    var pDay by remember { mutableStateOf("") }
    var pMonth by remember { mutableStateOf("") }
    var pYear by remember { mutableStateOf("") }
    var pHour by remember { mutableStateOf("") }
    var pMinute by remember { mutableStateOf("") }
    var pAmPm by remember { mutableStateOf("AM") }
    var pCityName by remember { mutableStateOf("") }
    var pTimezoneId by remember { mutableStateOf<String?>(null) }
    var pLatitude by remember { mutableStateOf<Double?>(null) }
    var pLongitude by remember { mutableStateOf<Double?>(null) }
    var pTimezone by remember { mutableStateOf<Double?>(null) }

    var locationPickerTarget by remember { mutableStateOf("me") } // "me" or "partner"

    // Logic State
    var isWaiting by remember { mutableStateOf(false) }
    var waitingSessionId by remember { mutableStateOf<String?>(null) }
    var waitTimeLeft by remember { mutableStateOf(30) }

    // Location Picker
    val placeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val d = result.data!!
            val fullName = d.getStringExtra("name") ?: ""
            val cityRes = d.getStringExtra("city") ?: ""
            val tzId = d.getStringExtra("timezoneId") ?: ""
            val latRes = d.getDoubleExtra("lat", 0.0)
            val lonRes = d.getDoubleExtra("lon", 0.0)

            if (locationPickerTarget == "me") {
                cityName = if (cityRes.isNotBlank()) cityRes else fullName
                timezoneId = tzId
                latitude = latRes
                longitude = lonRes
            } else {
                pCityName = if (cityRes.isNotBlank()) cityRes else fullName
                pTimezoneId = tzId
                pLatitude = latRes
                pLongitude = lonRes
            }
        }
    }

    val launchLocationPicker = { target: String ->
        locationPickerTarget = target
        val intent = Intent(context, com.astroeleven.app.ui.city.CitySearchActivity::class.java)
        placeLauncher.launch(intent)
    }

    LaunchedEffect(Unit) {
        var data = existingData
        if (data == null) {
            data = loadForm()
        }

        if (data != null) {
            val d = data!!
            name = d.optString("name")
            gender = d.optString("gender", "Male")
            cityName = d.optString("city")
            day = d.optInt("day", 0).toString().takeIf { it != "0" } ?: ""
            month = d.optInt("month", 0).toString().takeIf { it != "0" } ?: ""
            year = d.optInt("year", 0).toString().takeIf { it != "0" } ?: ""
            
            val h24 = d.optInt("hour", 12)
            if (h24 >= 12) {
                amPm = "PM"
                hour = (if (h24 > 12) h24 - 12 else 12).toString()
            } else {
                amPm = "AM"
                hour = (if (h24 == 0) 12 else h24).toString()
            }
            minute = d.optInt("minute", 0).toString().padStart(2, '0')
            
            latitude = d.optDouble("latitude", 0.0).takeIf { it != 0.0 }
            longitude = d.optDouble("longitude", 0.0).takeIf { it != 0.0 }
            timezoneId = d.optString("timezone").takeIf { it.isNotEmpty() }

            // Partner Data
            val pObj = d.optJSONObject("partnerData") ?: d.optJSONObject("partner")
            if (pObj != null) {
                pName = pObj.optString("name")
                pGender = pObj.optString("gender", "Female")
                
                pDay = pObj.optString("day").takeIf { it != "0" && it.isNotEmpty() } ?: ""
                pMonth = pObj.optString("month").takeIf { it != "0" && it.isNotEmpty() } ?: ""
                pYear = pObj.optString("year").takeIf { it != "0" && it.isNotEmpty() } ?: ""
                
                val ph24 = pObj.optInt("hour", 12)
                if (ph24 >= 12) {
                    pAmPm = "PM"
                    pHour = (if (ph24 > 12) ph24 - 12 else 12).toString()
                } else {
                    pAmPm = "AM"
                    pHour = (if (ph24 == 0) 12 else ph24).toString()
                }
                pMinute = pObj.optInt("minute", 0).toString().padStart(2, '0')
                
                pCityName = pObj.optString("city").ifEmpty { pObj.optString("pob") }
                pLatitude = pObj.optDouble("latitude", 0.0).takeIf { it != 0.0 } ?: pObj.optDouble("lat", 0.0).takeIf { it != 0.0 }
                pLongitude = pObj.optDouble("longitude", 0.0).takeIf { it != 0.0 } ?: pObj.optDouble("lon", 0.0).takeIf { it != 0.0 }
                pTimezoneId = pObj.optString("timezone")
            }
        }

        // Listen for astrologer response
        SocketManager.onSessionAnswered { response ->
            val accepted = response.optBoolean("accept")
            val sId = response.optString("sessionId")
            if (accepted && sId == waitingSessionId) {
                isWaiting = false
                onSessionConnected(sId, callType ?: "audio")
            } else if (!accepted && response.has("accept")) {
                isWaiting = false
                onUnanswered()
            }
        }
    }

    var isMatchingLocal by remember { mutableStateOf(isMatching) }

    fun submit() {
        if (name.isBlank() || cityName.isBlank() || day.isBlank() || month.isBlank() || year.isBlank() || hour.isBlank() || minute.isBlank()) {
            Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (isMatchingLocal && (pName.isBlank() || pCityName.isBlank() || pDay.isBlank() || pMonth.isBlank() || pYear.isBlank() || pHour.isBlank() || pMinute.isBlank())) {
            Toast.makeText(context, "Please fill all partner details", Toast.LENGTH_SHORT).show()
            return
        }
        val hour24 = if (amPm == "PM" && hour.toInt() < 12) hour.toInt() + 12 else if (amPm == "AM" && hour.toInt() == 12) 0 else hour.toInt()
        
        val payload = JSONObject().apply {
            put("name", name)
            put("gender", gender)
            put("day", day.toInt())
            put("month", month.toInt())
            put("year", year.toInt())
            put("hour", hour24)
            put("minute", minute.toInt())
            put("city", cityName)
            put("latitude", latitude)
            put("longitude", longitude)
            put("timezone", timezoneId)
            put("isMatching", isMatchingLocal)
            if (isMatchingLocal) {
               val partner = JSONObject()
               val pHour24 = if (pAmPm == "PM" && pHour.toInt() < 12) pHour.toInt() + 12 else if (pAmPm == "AM" && pHour.toInt() == 12) 0 else pHour.toInt()
               
               partner.put("name", pName)
               partner.put("gender", pGender)
               partner.put("day", pDay.toInt())
               partner.put("month", pMonth.toInt())
               partner.put("year", pYear.toInt())
               partner.put("hour", pHour24)
               partner.put("minute", pMinute.toInt())
               partner.put("city", pCityName)
               partner.put("latitude", pLatitude)
               partner.put("longitude", pLongitude)
               partner.put("timezone", pTimezoneId)
               put("partnerData", partner)
            }
        }
        
        // Save for next time
        saveForm(payload)
        
        if (isEditMode) {
            val resultIntent = Intent()
            resultIntent.putExtra("birthData", payload.toString())
            (context as Activity).setResult(Activity.RESULT_OK, resultIntent)
            (context as Activity).finish()
            return
        }

        // If it was opened from Home Page for free matching
        if (isMatching && partnerId == null) {
            val intent = Intent(context, com.astroeleven.app.ui.chart.MatchDisplayActivity::class.java)
            intent.putExtra("birthData", payload.toString())
            context.startActivity(intent)
            (context as Activity).finish()
            return
        }

        if (partnerId != null && callType != null) {
            SocketManager.init()
            SocketManager.ensureConnection()
            SocketManager.requestSession(partnerId, callType, payload) { response ->
                if (response?.optBoolean("ok") == true) {
                    waitingSessionId = response.optString("sessionId")
                    scope.launch { isWaiting = true }
                } else {
                    scope.launch {
                        val errMsg = response?.optString("error") ?: "Failed"
                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        if (errMsg.contains("Balance") || errMsg.contains("Insufficient")) {
                            val intent = Intent(context, com.astroeleven.app.ui.wallet.WalletActivity::class.java)
                            context.startActivity(intent)
                            (context as Activity).finish()
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicAppTheme.backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = Localization.get("premium_consultation", isTamil), 
                            color = CosmicAppTheme.colors.textPrimary, 
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CosmicAppTheme.colors.textPrimary)
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CosmicAppTheme.colors.textPrimary.copy(alpha = 0.1f))
                                .clickable { isTamil = !isTamil }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isTamil) "English" else "தமிழ்", 
                                color = CosmicAppTheme.colors.accent, // Rich Gold
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PREMIUM HEADER: If consulting with an Astrologer
                if (partnerId != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1428).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFFFFB300), CircleShape)
                                    .background(CosmicAppTheme.colors.textPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = partnerName.take(1).uppercase(),
                                    color = CosmicAppTheme.colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = partnerName,
                                    color = CosmicAppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isTamil) "இணைப்பில் உள்ளார்" else "Live Session",
                                        color = CosmicAppTheme.colors.textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = if (callType == "chat") Icons.Default.AutoAwesome else Icons.Default.AutoFixHigh
                                Icon(icon, "Type", tint = CosmicAppTheme.colors.accent, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // PERSONAL DETAILS SECTION CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, CosmicAppTheme.colors.textSecondary.copy(alpha = 0.15f)), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicAppTheme.colors.cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = Localization.get("personal_details", isTamil),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CosmicAppTheme.colors.accent,
                            letterSpacing = 0.5.sp
                        )

                        val textFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CosmicAppTheme.colors.textPrimary, 
                            unfocusedTextColor = CosmicAppTheme.colors.textPrimary,
                            disabledTextColor = CosmicAppTheme.colors.textSecondary,
                            focusedBorderColor = CosmicAppTheme.colors.accent,
                            unfocusedBorderColor = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.3f),
                            disabledBorderColor = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.15f),
                            cursorColor = CosmicAppTheme.colors.accent,
                            focusedContainerColor = Color(0xFFF4F4F4),
                            unfocusedContainerColor = Color(0xFFF4F4F4),
                            disabledContainerColor = Color(0xFFF4F4F4)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(Localization.get("full_name", isTamil), color = CosmicAppTheme.colors.textSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )

                        // Segmented Gender Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = Localization.get("gender", isTamil),
                                color = CosmicAppTheme.colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF4F4F4))
                                    .border(1.dp, CosmicAppTheme.colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .run {
                                            if (gender == "Male") {
                                                background(Brush.horizontalGradient(listOf(Color(0xFFFDBA16), Color(0xFFE1353C))))
                                            } else {
                                                this
                                            }
                                        }
                                        .clickable { gender = "Male" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Localization.get("male", isTamil),
                                        color = if (gender == "Male") Color.White else CosmicAppTheme.colors.textSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .run {
                                            if (gender == "Female") {
                                                background(Brush.horizontalGradient(listOf(Color(0xFFFDBA16), Color(0xFFE1353C))))
                                            } else {
                                                this
                                            }
                                        }
                                        .clickable { gender = "Female" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Localization.get("female", isTamil),
                                        color = if (gender == "Female") Color.White else CosmicAppTheme.colors.textSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Date of Birth
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = Localization.get("dob", isTamil),
                                color = CosmicAppTheme.colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = day,
                                    onValueChange = { if(it.length <= 2) day = it },
                                    placeholder = { Text("DD", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = month,
                                    onValueChange = { if(it.length <= 2) month = it },
                                    placeholder = { Text("MM", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = year,
                                    onValueChange = { if(it.length <= 4) year = it },
                                    placeholder = { Text("YYYY", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1.3f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        DatePickerDialog(context, com.astroeleven.app.R.style.DialogPickerTheme, { _, py, pm, pd ->
                                            year = py.toString(); month = (pm + 1).toString(); day = pd.toString()
                                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                    },
                                    modifier = Modifier.size(54.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.15f)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, "Pick Date", tint = CosmicAppTheme.colors.accent, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        // Time of Birth
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = Localization.get("tob", isTamil),
                                color = CosmicAppTheme.colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = hour,
                                    onValueChange = { if(it.length <= 2) hour = it },
                                    placeholder = { Text("HH", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = minute,
                                    onValueChange = { if(it.length <= 2) minute = it },
                                    placeholder = { Text("MM", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    singleLine = true
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF4F4F4))
                                        .border(1.dp, CosmicAppTheme.colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .clickable { amPm = if (amPm == "AM") "PM" else "AM" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(amPm, color = CosmicAppTheme.colors.accent, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                }
                                Button(
                                    onClick = {
                                        TimePickerDialog(context, com.astroeleven.app.R.style.DialogPickerTheme, { _, ph, pm ->
                                            val hTyped = if (ph > 12) (ph - 12) else if (ph == 0) 12 else ph
                                            hour = hTyped.toString(); minute = String.format("%02d", pm); amPm = if (ph >= 12) "PM" else "AM"
                                        }, 12, 0, false).show()
                                    },
                                    modifier = Modifier.size(54.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.15f)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, "Pick Time", tint = CosmicAppTheme.colors.accent, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        // Place of Birth
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = Localization.get("pob", isTamil),
                                color = CosmicAppTheme.colors.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { launchLocationPicker("me") }
                            ) {
                                OutlinedTextField(
                                    value = cityName,
                                    onValueChange = {},
                                    placeholder = { Text(Localization.get("city", isTamil), color = CosmicAppTheme.colors.textSecondary) },
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.LocationOn, "Location", tint = CosmicAppTheme.colors.accent, modifier = Modifier.size(22.dp)) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Matchmaking Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.04f))
                                .border(1.dp, CosmicAppTheme.colors.textSecondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable { isMatchingLocal = !isMatchingLocal }
                                .padding(14.dp)
                        ) {
                            Text(
                                text = if (isTamil) "திருமணப் பொருத்தம் விவரங்களைச் சேர்க்க" else "Add Marriage Matching Details",
                                color = CosmicAppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isMatchingLocal,
                                onCheckedChange = { isMatchingLocal = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CosmicAppTheme.colors.accent,
                                    uncheckedThumbColor = CosmicAppTheme.colors.textSecondary,
                                    uncheckedTrackColor = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.2f)
                                )
                            )
                        }

                        if (isMatchingLocal) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.15f), thickness = 1.dp)
                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = if (isTamil) "துணை விவரங்கள்" else "Partner Details",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CosmicAppTheme.colors.accent,
                                letterSpacing = 0.5.sp
                            )

                            OutlinedTextField(
                                value = pName,
                                onValueChange = { pName = it },
                                label = { Text(if (isTamil) "துணையின் பெயர்" else "Partner's Full Name", color = CosmicAppTheme.colors.textSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )

                            // Partner Gender Segmented Control
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = Localization.get("gender", isTamil),
                                    color = CosmicAppTheme.colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF4F4F4))
                                        .border(1.dp, CosmicAppTheme.colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                     Box(
                                         modifier = Modifier
                                             .weight(1f)
                                             .fillMaxHeight()
                                             .clip(RoundedCornerShape(12.dp))
                                             .run {
                                                 if (pGender == "Male") {
                                                     background(Brush.horizontalGradient(listOf(Color(0xFFFDBA16), Color(0xFFE1353C))))
                                                 } else {
                                                     this
                                                 }
                                             }
                                             .clickable { pGender = "Male" },
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Text(
                                             text = Localization.get("male", isTamil),
                                             color = if (pGender == "Male") Color.White else CosmicAppTheme.colors.textSecondary,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 14.sp
                                         )
                                     }
                                     Box(
                                         modifier = Modifier
                                             .weight(1f)
                                             .fillMaxHeight()
                                             .clip(RoundedCornerShape(12.dp))
                                             .run {
                                                 if (pGender == "Female") {
                                                     background(Brush.horizontalGradient(listOf(Color(0xFFFDBA16), Color(0xFFE1353C))))
                                                 } else {
                                                     this
                                                 }
                                             }
                                             .clickable { pGender = "Female" },
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Text(
                                             text = Localization.get("female", isTamil),
                                             color = if (pGender == "Female") Color.White else CosmicAppTheme.colors.textSecondary,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 14.sp
                                         )
                                     }
                                }
                            }

                            // Partner DOB
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = Localization.get("dob", isTamil),
                                    color = CosmicAppTheme.colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = pDay,
                                        onValueChange = { if(it.length <= 2) pDay = it },
                                        placeholder = { Text("DD", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = textFieldColors,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = pMonth,
                                        onValueChange = { if(it.length <= 2) pMonth = it },
                                        placeholder = { Text("MM", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = textFieldColors,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = pYear,
                                        onValueChange = { if(it.length <= 4) pYear = it },
                                        placeholder = { Text("YYYY", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                        modifier = Modifier.weight(1.3f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = textFieldColors,
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            DatePickerDialog(context, com.astroeleven.app.R.style.DialogPickerTheme, { _, py, pm, pd ->
                                                pYear = py.toString(); pMonth = (pm + 1).toString(); pDay = pd.toString()
                                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                        },
                                        modifier = Modifier.size(54.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.15f)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, "Pick Date", tint = CosmicAppTheme.colors.accent, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }

                            // Partner TOB
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = Localization.get("tob", isTamil),
                                    color = CosmicAppTheme.colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = pHour,
                                        onValueChange = { if(it.length <= 2) pHour = it },
                                        placeholder = { Text("HH", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = textFieldColors,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = pMinute,
                                        onValueChange = { if(it.length <= 2) pMinute = it },
                                        placeholder = { Text("MM", color = CosmicAppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = textFieldColors,
                                        singleLine = true
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF4F4F4))
                                            .border(1.dp, CosmicAppTheme.colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .clickable { pAmPm = if (pAmPm == "AM") "PM" else "AM" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(pAmPm, color = CosmicAppTheme.colors.accent, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                    }
                                    Button(
                                        onClick = {
                                            TimePickerDialog(context, com.astroeleven.app.R.style.DialogPickerTheme, { _, ph, pm ->
                                                val hTyped = if (ph > 12) (ph - 12) else if (ph == 0) 12 else ph
                                                pHour = hTyped.toString(); pMinute = String.format("%02d", pm); pAmPm = if (ph >= 12) "PM" else "AM"
                                            }, 12, 0, false).show()
                                        },
                                        modifier = Modifier.size(54.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.15f)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, "Pick Time", tint = CosmicAppTheme.colors.accent, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }

                            // Partner Place of Birth
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = Localization.get("pob", isTamil),
                                    color = CosmicAppTheme.colors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { launchLocationPicker("partner") }
                                ) {
                                    OutlinedTextField(
                                        value = pCityName,
                                        onValueChange = {},
                                        placeholder = { Text(Localization.get("city", isTamil), color = CosmicAppTheme.colors.textSecondary) },
                                        readOnly = true,
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = { Icon(Icons.Default.LocationOn, "Location", tint = CosmicAppTheme.colors.accent, modifier = Modifier.size(22.dp)) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = textFieldColors
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Glowing Submit Button
                        Button(
                            onClick = { submit() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFFB300), // Pure Gold
                                                Color(0xFFFF7F00)  // Deep Orange
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val btnText = if (isMatching) {
                                    if (isTamil) "பொருத்தம் பார்க்க" else "START MATCHING"
                                } else if (isEditMode) {
                                    if (isTamil) "விவரங்களை புதுப்பிக்க" else "UPDATE DETAILS"
                                } else {
                                    if (isTamil) "ஆலோசனை தொடங்க" else "START CONSULTATION"
                                }
                                Text(
                                    text = btnText, 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Text(
                            text = if (isTamil) "அனைத்து விவரங்களையும் சரிபார்க்கவும்" else "Please verify all details before submitting",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicAppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Wait / Connecting dialogue
        if (isWaiting) {
            DisposableEffect(Unit) {
                val toneGen = try {
                    android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100).apply {
                        startTone(android.media.ToneGenerator.TONE_SUP_RINGTONE, 30000)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                onDispose {
                    try {
                        toneGen?.stopTone()
                        toneGen?.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            LaunchedEffect(isWaiting) {
                waitTimeLeft = 30
                while (waitTimeLeft > 0 && isWaiting) {
                    delay(1000)
                    waitTimeLeft--
                }
                if (waitTimeLeft <= 0) {
                    onUnanswered()
                    isWaiting = false
                }
            }

            Dialog(onDismissRequest = { 
                if (waitingSessionId != null) {
                    val endPayload = org.json.JSONObject().apply { put("sessionId", waitingSessionId) }
                    com.astroeleven.app.data.remote.SocketManager.emitReliable("end-session", endPayload)
                }
                isWaiting = false 
            }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(
                            1.dp, 
                            Brush.linearGradient(listOf(Color(0xFFFDBA16), Color(0xFFE1353C))), 
                            RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicAppTheme.colors.cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { waitTimeLeft / 30f },
                                modifier = Modifier.size(90.dp),
                                color = CosmicAppTheme.colors.accent,
                                strokeWidth = 5.dp,
                                trackColor = Color(0xFFEEEEEE)
                            )
                            Text(
                                text = waitTimeLeft.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = CosmicAppTheme.colors.accent,
                                fontSize = 22.sp
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = Localization.get("connecting_title", isTamil),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = CosmicAppTheme.colors.textPrimary,
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp
                            )
                            Text(
                                text = Localization.get("connecting_subtitle", isTamil),
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicAppTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { 
                                if (waitingSessionId != null) {
                                    val endPayload = org.json.JSONObject().apply { put("sessionId", waitingSessionId) }
                                    com.astroeleven.app.data.remote.SocketManager.emitReliable("end-session", endPayload)
                                }
                                isWaiting = false 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text(
                                text = Localization.get("cancel_request", isTamil), 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildPlaceName(city: String, state: String, country: String): String {
    return listOf(city, state, country).filter { it.isNotBlank() }.joinToString(", ")
}

private fun parsePlaceName(place: String): Triple<String, String, String> {
    if (place.isBlank()) return Triple("", "", "")
    val parts = place.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return Triple(parts.getOrNull(0) ?: "", parts.getOrNull(1) ?: "", parts.getOrNull(2) ?: "")
}
