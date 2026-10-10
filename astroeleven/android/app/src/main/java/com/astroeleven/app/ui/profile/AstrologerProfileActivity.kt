package com.astroeleven.app.ui.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astroeleven.app.R
import com.astroeleven.app.ui.theme.CosmicAppTheme
import com.astroeleven.app.ui.theme.AstroDimens
import coil.compose.AsyncImage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect

class AstrologerProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var astroId = (intent.getStringExtra("astro_id")
            ?: intent.getStringExtra("id")
            ?: intent.getStringExtra("userId")
            ?: "").trim()
        val initialName = intent.getStringExtra("astro_name") ?: ""
        val initialExp = intent.getStringExtra("astro_exp") ?: ""
        val initialSkills = intent.getStringExtra("astro_skills") ?: ""
        val initialImage = intent.getStringExtra("astro_image") ?: ""
        val initialPrice = intent.getIntExtra("astro_price", 15)
        val initialChatPrice = intent.getIntExtra("chat_price", 15)
        val initialCallPrice = intent.getIntExtra("call_price", 15)
        val initialVideoPrice = intent.getIntExtra("video_price", 20)
        val initialChatOnline = intent.getBooleanExtra("is_chat_online", false)
        val initialAudioOnline = intent.getBooleanExtra("is_audio_online", false)
        val initialVideoOnline = intent.getBooleanExtra("is_video_online", false)

        // Parse deep link URI if launched from URL (e.g., https://astroeleven.com/astrologer/123 or astroeleven://astrologer/123)
        val uri = intent.data
        if (uri != null) {
            val path = uri.path ?: ""
            val queryAstro = uri.getQueryParameter("astro") ?: uri.getQueryParameter("id")
            val extractedId = when {
                !queryAstro.isNullOrEmpty() -> queryAstro
                path.contains("/astrologer/") -> path.substringAfter("/astrologer/").trim('/')
                uri.host == "astrologer" -> path.trim('/')
                else -> uri.lastPathSegment ?: ""
            }
            if (extractedId.isNotEmpty() && extractedId != "astrologer") {
                astroId = extractedId
            }
        }

        setContent {
            CosmicAppTheme(forceLight = true) {
                var currentAstroId by remember { mutableStateOf(astroId) }
                var currentName by remember { mutableStateOf(if (initialName.isNotEmpty()) initialName else "Astrologer") }
                var currentExp by remember { mutableStateOf(if (initialExp.isNotEmpty()) initialExp else "5") }
                var currentSkills by remember { mutableStateOf(if (initialSkills.isNotEmpty()) initialSkills else "Vedic, Tarot") }
                var currentImage by remember { mutableStateOf(initialImage) }
                var currentPrice by remember { mutableStateOf(initialPrice) }
                var currentChatPrice by remember { mutableStateOf(initialChatPrice) }
                var currentCallPrice by remember { mutableStateOf(initialCallPrice) }
                var currentVideoPrice by remember { mutableStateOf(initialVideoPrice) }
                var currentChatOnline by remember { mutableStateOf(initialChatOnline) }
                var currentAudioOnline by remember { mutableStateOf(initialAudioOnline) }
                var currentVideoOnline by remember { mutableStateOf(initialVideoOnline) }
                var isLoading by remember { mutableStateOf(initialName.isEmpty() && astroId.isNotEmpty()) }

                LaunchedEffect(astroId) {
                    if (astroId.isNotEmpty() && (initialName.isEmpty() || initialName == "Astrologer")) {
                        withContext(Dispatchers.IO) {
                            try {
                                val url = "${com.astroeleven.app.utils.Constants.SERVER_URL}/api/astrology/astrologer/$astroId"
                                val client = okhttp3.OkHttpClient()
                                val req = okhttp3.Request.Builder().url(url).get().build()
                                client.newCall(req).execute().use { resp ->
                                    if (resp.isSuccessful) {
                                        val jsonStr = resp.body?.string() ?: ""
                                        val obj = org.json.JSONObject(jsonStr)
                                        if (obj.optBoolean("ok") && obj.has("astrologer")) {
                                            val a = obj.getJSONObject("astrologer")
                                            val fetchedId = a.optString("userId", "").ifEmpty { a.optString("id", "") }
                                            if (fetchedId.isNotEmpty()) {
                                                currentAstroId = fetchedId
                                            }
                                            currentName = a.optString("name", currentName)
                                            currentExp = a.optInt("experience", 5).toString()
                                            val skillsArr = a.optJSONArray("skills")
                                            if (skillsArr != null && skillsArr.length() > 0) {
                                                val sList = mutableListOf<String>()
                                                for (i in 0 until skillsArr.length()) sList.add(skillsArr.getString(i))
                                                currentSkills = sList.joinToString(", ")
                                            }
                                            currentImage = a.optString("image", currentImage)
                                            currentPrice = a.optInt("price", 15)
                                            currentChatPrice = a.optInt("chatPrice", currentPrice)
                                            currentCallPrice = a.optInt("callPrice", currentPrice)
                                            currentVideoPrice = a.optInt("videoPrice", 20)
                                            currentChatOnline = a.optBoolean("isChatOnline", false)
                                            currentAudioOnline = a.optBoolean("isAudioOnline", false)
                                            currentVideoOnline = a.optBoolean("isVideoOnline", false)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AstrologerProfile", "Failed to load astro details: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CosmicAppTheme.backgroundBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFE87A1E))
                    }
                } else {
                    AstrologerProfileScreen(
                        id = currentAstroId,
                        name = currentName,
                        exp = currentExp,
                        skills = currentSkills,
                        image = currentImage,
                        price = currentPrice,
                        chatPrice = currentChatPrice,
                        callPrice = currentCallPrice,
                        videoPrice = currentVideoPrice,
                        isChatOnline = currentChatOnline,
                        isAudioOnline = currentAudioOnline,
                        isVideoOnline = currentVideoOnline,
                        onBack = { finish() },
                        onAction = { type ->
                            val intent = android.content.Intent(this@AstrologerProfileActivity, com.astroeleven.app.ui.intake.IntakeActivity::class.java).apply {
                                putExtra("partnerId", currentAstroId)
                                putExtra("partnerName", currentName)
                                putExtra("partnerImage", currentImage)
                                putExtra("type", type)
                                val selectedPrice = when(type) {
                                    "chat" -> currentChatPrice
                                    "audio" -> currentCallPrice
                                    "video" -> currentVideoPrice
                                    else -> currentPrice
                                }
                                putExtra("partnerPrice", selectedPrice)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstrologerProfileScreen(
    id: String,
    name: String,
    exp: String,
    skills: String,
    image: String,
    price: Int,
    chatPrice: Int,
    callPrice: Int,
    videoPrice: Int,
    isChatOnline: Boolean,
    isAudioOnline: Boolean,
    isVideoOnline: Boolean,
    onBack: () -> Unit,
    onAction: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val peacockTeal = Color(0xFFE87A1E)
    val yellowAccent = Color(0xFFFFD54F)

    var isLiked by remember { mutableStateOf(false) }

    val systemUiController = remember { (context as? Activity)?.window }
    SideEffect {
        systemUiController?.statusBarColor = android.graphics.Color.parseColor("#E87A1E")
        systemUiController?.navigationBarColor = android.graphics.Color.parseColor("#E87A1E")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isLiked = !isLiked
                        android.widget.Toast.makeText(
                            context,
                            if (isLiked) "Added $name to Favorites ❤️" else "Removed $name from Favorites",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isLiked) Color(0xFFFF4081) else Color.White
                        )
                    }
                    IconButton(onClick = {
                        val cleanId = id.trim()
                        val shareUrl = if (cleanId.isNotEmpty()) "https://astroeleven.com/astrologer/${cleanId}" else "https://astroeleven.com"
                        val shareText = "🌟 Consult with ${name} on Astro Eleven for accurate life predictions & guidance!\n\n${shareUrl}"
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Consult with $name on Astro Eleven")
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Astrologer Profile"))
                    }) {
                        Icon(Icons.Default.Share, "Share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFE87A1E))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .background(CosmicAppTheme.backgroundBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Header with Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(CosmicAppTheme.backgroundBrush)
                )

                // Avatar
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .align(Alignment.BottomCenter)
                        .shadow(8.dp, CircleShape)
                ) {
                    val imageUrl = if (image.startsWith("http")) image
                                  else if (image.isNotEmpty()) {
                                      val path = if (image.startsWith("/")) image else "/${image}"
                                      "${com.astroeleven.app.utils.Constants.SERVER_URL}$path"
                                  }
                                  else ""
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(CosmicAppTheme.colors.bgStart)
                            .border(3.dp, CosmicAppTheme.colors.accent.copy(alpha = 0.5f), CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_person_placeholder),
                        placeholder = painterResource(id = R.drawable.ic_person_placeholder)
                    )
                    // Verified Badge
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(CosmicAppTheme.colors.bgStart, CircleShape)
                            .border(2.dp, CosmicAppTheme.colors.accent.copy(alpha = 0.3f), CircleShape)
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = CosmicAppTheme.colors.textPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=4.dp)) {
                    Text("★★★★★", color = Color(0xFFFFC107), fontSize = 16.sp)
                }

                Text(
                    text = skills,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicAppTheme.colors.textSecondary,
                    modifier = Modifier.padding(top=6.dp),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isChatOnline) {
                        Surface(
                            shape = RoundedCornerShape(AstroDimens.RadiusSmall),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFF81C784)),
                        ) {
                            Text(
                                text = "Chat: ₹$chatPrice/min",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (isAudioOnline) {
                        Surface(
                            shape = RoundedCornerShape(AstroDimens.RadiusSmall),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                        ) {
                            Text(
                                text = "Call: ₹$callPrice/min",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (isVideoOnline) {
                        Surface(
                            shape = RoundedCornerShape(AstroDimens.RadiusSmall),
                            color = Color(0xFFE1F5FE),
                            border = BorderStroke(1.dp, Color(0xFF4FC3F7)),
                        ) {
                            Text(
                                text = "Video: ₹$videoPrice/min",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0277BD),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (!isChatOnline && !isAudioOnline && !isVideoOnline) {
                        Surface(
                            shape = RoundedCornerShape(AstroDimens.RadiusSmall),
                            color = CosmicAppTheme.colors.accent.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, CosmicAppTheme.colors.accent.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = "Rate: ₹$price/min",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CosmicAppTheme.colors.accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Stats Section
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(vertical = AstroDimens.Medium)
                       .clip(RoundedCornerShape(AstroDimens.RadiusMedium))
                       .background(CosmicAppTheme.colors.cardBg)
                       .border(1.dp, CosmicAppTheme.colors.cardStroke.copy(alpha = 0.2f), RoundedCornerShape(AstroDimens.RadiusMedium))
                       .padding(AstroDimens.Medium),
                   horizontalArrangement = Arrangement.SpaceEvenly,
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(icon = Icons.Default.Chat, value = "49k Mins")
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(CosmicAppTheme.colors.cardStroke.copy(alpha=0.3f)))
                    StatItem(icon = Icons.Default.Call, value = "31k Mins")
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(CosmicAppTheme.colors.cardStroke.copy(alpha=0.3f)))
                    StatItem(icon = Icons.Default.CheckCircle, value = "$exp Years")
                }

                // Bio Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicAppTheme.colors.cardBg),
                    shape = RoundedCornerShape(AstroDimens.RadiusMedium),
                    border = BorderStroke(1.dp, CosmicAppTheme.colors.cardStroke.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(AstroDimens.Medium)) {
                        Text("About Astrologer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CosmicAppTheme.colors.accent)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$name is highly experienced in $skills. Dedicated to providing accurate guidance and helping clients find clarity in life's complex situations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicAppTheme.colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isChatOnline) {
                        ActionButton(
                            icon = Icons.Default.Chat,
                            label = "Chat (₹$chatPrice)",
                            color = CosmicAppTheme.colors.accent,
                            isEnabled = true,
                            onClick = { onAction("chat") }
                        )
                    }

                    if (isAudioOnline) {
                        ActionButton(
                            icon = Icons.Default.Call,
                            label = "Call (₹$callPrice)",
                            color = CosmicAppTheme.colors.accent,
                            isEnabled = true,
                            onClick = { onAction("audio") }
                        )
                    }

                    if (isVideoOnline) {
                        ActionButton(
                            icon = androidx.compose.material.icons.Icons.Rounded.VideoCall,
                            label = "Video (₹$videoPrice)",
                            color = CosmicAppTheme.colors.accent,
                            isEnabled = true,
                            onClick = { onAction("video") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        isLiked = !isLiked
                        android.widget.Toast.makeText(
                            context,
                            if (isLiked) "Following $name ❤️" else "Unfollowed $name",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isLiked) Color(0xFF2E7D32) else Color(0xFFE87A1E)),
                    shape = RoundedCornerShape(AstroDimens.RadiusMedium)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLiked) "Following $name" else "Follow $name",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }

                // Reviews Section Placeholder removed
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = CosmicAppTheme.colors.accent, modifier = Modifier.size(24.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CosmicAppTheme.colors.textPrimary, modifier = Modifier.padding(top=4.dp))
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, color: Color, isEnabled: Boolean, onClick: () -> Unit) {
    val finalColor = if (isEnabled) color else Color.Gray
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = isEnabled,
            modifier = Modifier
                .size(56.dp)
                .background(finalColor.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, finalColor.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = finalColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = finalColor)
    }
}
