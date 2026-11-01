package com.astroeleven.app.ui.astro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.astroeleven.app.data.local.TokenManager
import com.astroeleven.app.ui.theme.CosmicAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AstrologerHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenManager = TokenManager(this)
        val session = tokenManager.getUserSession()
        val userId = session?.userId ?: ""
        val isTamil = intent.getBooleanExtra("isTamil", false)

        setContent {
            CosmicAppTheme {
                HistoryScreen(
                    userId = userId,
                    isTamil = isTamil,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    userId: String,
    isTamil: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf<List<SessionHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedChatSession by remember { mutableStateOf<SessionHistoryItem?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(userId, refreshTrigger) {
        isLoading = true
        error = null
        withContext(Dispatchers.IO) {
            try {
                val myRole = TokenManager(context).getUserSession()?.role ?: "client"
                val client = okhttp3.OkHttpClient()

                val request = okhttp3.Request.Builder()
                    .url("${com.astroeleven.app.utils.Constants.SERVER_URL}/api/astrology/history/$userId")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "{}")
                    if (json.optBoolean("ok")) {
                        val array = json.optJSONArray("sessions") ?: JSONArray()
                        val list = mutableListOf<SessionHistoryItem>()

                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val isAstro = myRole == "astrologer"
                            val sId = if (obj.has("sessionId") && obj.optString("sessionId").isNotEmpty()) {
                                obj.optString("sessionId")
                            } else if (obj.has("id") && obj.optString("id").isNotEmpty()) {
                                obj.optString("id")
                            } else {
                                obj.optString("_id", "")
                            }

                            val pName = if (isAstro) {
                                val c = obj.optString("clientName", "")
                                if (c.isBlank() || c.equals("Unknown Client", ignoreCase = true)) {
                                    if (isTamil) "பயனர்" else "Client"
                                } else c
                            } else {
                                val a = obj.optString("astrologerName", "")
                                if (a.isBlank() || a.equals("Unknown Astrologer", ignoreCase = true)) {
                                    if (isTamil) "ஜோதிடர்" else "Astrologer"
                                } else a
                            }

                            list.add(
                                SessionHistoryItem(
                                    id = sId,
                                    partnerName = pName,
                                    type = obj.optString("type", "call"),
                                    startTime = if (obj.has("actualBillingStart") && obj.optLong("actualBillingStart") > 0) {
                                        obj.optLong("actualBillingStart")
                                    } else {
                                        obj.optLong("startTime", 0)
                                    },
                                    endTime = if (obj.has("sessionEndAt") && obj.optLong("sessionEndAt") > 0) {
                                        obj.optLong("sessionEndAt")
                                    } else {
                                        obj.optLong("endTime", 0)
                                    },
                                    duration = obj.optInt("duration", 0),
                                    amount = if (isAstro) obj.optDouble("totalEarned", 0.0) else obj.optDouble("totalCharged", 0.0),
                                    isEarned = isAstro
                                )
                            )
                        }
                        sessions = list
                    } else {
                        error = if (isTamil) "வரலாற்றை ஏற்ற முடியவில்லை" else "Failed to load history"
                    }
                } else {
                    error = "Server error: ${response.code}"
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isTamil) "ஆலோசனை & அரட்டை வரலாறு" else "Consultation & Chat History",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicAppTheme.colors.headerStart
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(CosmicAppTheme.backgroundBrush)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CosmicAppTheme.colors.accent
                )
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = error!!, color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { refreshTrigger++ }) {
                        Text(if (isTamil) "மீண்டும் முயற்சிக்கவும்" else "Retry")
                    }
                }
            } else if (sessions.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isTamil) "ஆலோசனை வரலாறு எதுவும் இல்லை" else "No consultation history found",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isTamil) "நீங்கள் ஜோதிடர்களுடன் உரையாடிய விவரங்கள் இங்கே தோன்றும்" else "Your completed audio, video and chat sessions with astrologers will appear here",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions) { session ->
                        HistoryCard(
                            item = session,
                            isTamil = isTamil,
                            onChatClick = {
                                selectedChatSession = session
                            }
                        )
                    }
                }
            }

            // Chat History Viewer Dialog
            selectedChatSession?.let { chatSession ->
                ChatHistoryDialog(
                    session = chatSession,
                    myUserId = userId,
                    isTamil = isTamil,
                    onDismiss = { selectedChatSession = null }
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: SessionHistoryItem,
    isTamil: Boolean = false,
    onChatClick: () -> Unit
) {
    val colors = CosmicAppTheme.colors
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val startTimeStr = if (item.startTime > 0) dateFormat.format(Date(item.startTime)) else "N/A"
    val isChat = item.type.equals("chat", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isChat) Modifier.clickable { onChatClick() } else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.accent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isChat) Icons.Default.Chat else Icons.Default.Call,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.partnerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isTamil) {
                            if (isChat) "அரட்டை ஆலோசனை (Chat)" else "அழைப்பு ஆலோசனை (Call)"
                        } else {
                            if (isChat) "Chat Session" else "Call Session"
                        },
                        fontSize = 11.sp,
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (item.isEarned) {
                            "+₹${String.format(Locale.US, "%.2f", item.amount)}"
                        } else {
                            "₹${String.format(Locale.US, "%.2f", item.amount)}"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (item.isEarned) Color(0xFF4CAF50) else colors.textPrimary
                    )
                    Text(
                        text = if (item.isEarned) {
                            if (isTamil) "சம்பாதித்தது" else "Earned"
                        } else {
                            if (isTamil) "செலுத்தியது" else "Paid"
                        },
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.cardStroke.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = if (isTamil) "தேதி & நேரம்" else "Date & Time",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    Text(startTimeStr, fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isTamil) "ஆலோசனை நேரம்" else "Consultation Time",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    val totalSec = if (item.duration > 50000) item.duration / 1000 else item.duration
                    val mins = totalSec / 60
                    val secs = totalSec % 60
                    val duraText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                    Text(duraText, fontSize = 13.sp, color = Color(0xFF039BE5), fontWeight = FontWeight.Bold)
                }
            }

            if (isChat) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onChatClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent.copy(alpha = 0.15f),
                        contentColor = colors.accent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTamil) "சாட் உரையாடலை காண்க" else "View Chat Messages",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.accent
                    )
                }
            }
        }
    }
}

@Composable
fun ChatHistoryDialog(
    session: SessionHistoryItem,
    myUserId: String,
    isTamil: Boolean = false,
    onDismiss: () -> Unit
) {
    val colors = CosmicAppTheme.colors
    var messages by remember { mutableStateOf<List<ChatHistoryMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session.id) {
        withContext(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val url = "${com.astroeleven.app.utils.Constants.SERVER_URL}/api/chat/history/${session.id}"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "{}")
                    if (json.optBoolean("ok")) {
                        val arr = json.optJSONArray("messages") ?: JSONArray()
                        val list = mutableListOf<ChatHistoryMessage>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                ChatHistoryMessage(
                                    id = obj.optString("messageId", UUID.randomUUID().toString()),
                                    fromUserId = obj.optString("fromUserId"),
                                    toUserId = obj.optString("toUserId"),
                                    text = obj.optString("text"),
                                    type = obj.optString("type", "text"),
                                    fileUrl = obj.optString("fileUrl"),
                                    timestamp = obj.optLong("timestamp", 0)
                                )
                            )
                        }
                        messages = list
                    } else {
                        error = json.optString("error", if (isTamil) "அரட்டை செய்திகளை ஏற்ற முடியவில்லை" else "Failed to load chat history")
                    }
                } else {
                    error = "HTTP ${response.code}"
                }
            } catch (e: Exception) {
                error = e.message ?: (if (isTamil) "இணைப்பு பிழை" else "Network error")
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = colors.bgStart,
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.4f)),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(colors.accent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.partnerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isTamil) "முந்தைய அரட்டை விவரங்கள்" else "Chat History Transcript",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textPrimary
                        )
                    }
                }

                HorizontalDivider(color = colors.cardStroke.copy(alpha = 0.5f))

                // Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CosmicAppTheme.backgroundBrush)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (error != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isTamil) "செய்திகளை ஏற்ற முடியவில்லை" else "Unable to load chat",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = error ?: "",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    } else if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = colors.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isTamil) "இந்த அரட்டையில் செய்திகள் எதுவும் பதிவு செய்யப்படவில்லை" else "No messages found in this chat session",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages) { msg ->
                                val isMe = msg.fromUserId == myUserId
                                val align = if (isMe) Alignment.End else Alignment.Start
                                val bubbleBg = if (isMe) colors.accent.copy(alpha = 0.22f) else colors.cardBg
                                val stroke = if (isMe) BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)) else BorderStroke(1.dp, colors.cardStroke)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = align
                                ) {
                                    Text(
                                        text = if (isMe) (if (isTamil) "நீங்கள்" else "You") else session.partnerName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isMe) colors.accent else colors.textSecondary,
                                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isMe) 14.dp else 2.dp,
                                            bottomEnd = if (isMe) 2.dp else 14.dp
                                        ),
                                        color = bubbleBg,
                                        border = stroke,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            if (msg.fileUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = msg.fileUrl,
                                                    contentDescription = "Shared Media",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 200.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .padding(bottom = 6.dp),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            if (msg.text.isNotBlank()) {
                                                Text(
                                                    text = msg.text,
                                                    color = colors.textPrimary,
                                                    fontSize = 14.sp,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                            if (msg.timestamp > 0) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = timeFormat.format(Date(msg.timestamp)),
                                                    color = colors.textSecondary,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.align(Alignment.End)
                                                )
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
}

data class ChatHistoryMessage(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val text: String,
    val type: String,
    val fileUrl: String,
    val timestamp: Long
)

data class SessionHistoryItem(
    val id: String,
    val partnerName: String,
    val type: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Int,
    val amount: Double,
    val isEarned: Boolean
)
