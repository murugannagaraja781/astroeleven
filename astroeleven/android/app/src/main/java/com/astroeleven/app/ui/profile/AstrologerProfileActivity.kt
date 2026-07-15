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
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.*
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

class AstrologerProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val astroName = intent.getStringExtra("astro_name") ?: "Astrologer"
        val astroExp = intent.getStringExtra("astro_exp") ?: "5"
        val astroSkills = intent.getStringExtra("astro_skills") ?: "Vedic, Tarot"
        val astroId = intent.getStringExtra("astro_id") ?: ""
        val astroImage = intent.getStringExtra("astro_image") ?: ""
        val astroPrice = intent.getIntExtra("astro_price", 15)
        val chatPrice = intent.getIntExtra("chat_price", 15)
        val callPrice = intent.getIntExtra("call_price", 15)
        val videoPrice = intent.getIntExtra("video_price", 20)
        val isChatOnline = intent.getBooleanExtra("is_chat_online", false)
        val isAudioOnline = intent.getBooleanExtra("is_audio_online", false)
        val isVideoOnline = intent.getBooleanExtra("is_video_online", false)

        setContent {
            CosmicAppTheme(forceLight = true) {
                AstrologerProfileScreen(
                    id = astroId,
                    name = astroName,
                    exp = astroExp,
                    skills = astroSkills,
                    image = astroImage,
                    price = astroPrice,
                    chatPrice = chatPrice,
                    callPrice = callPrice,
                    videoPrice = videoPrice,
                    isChatOnline = isChatOnline,
                    isAudioOnline = isAudioOnline,
                    isVideoOnline = isVideoOnline,
                    onBack = { finish() },
                    onAction = { type ->
                        val intent = android.content.Intent(this, com.astroeleven.app.ui.intake.IntakeActivity::class.java).apply {
                            putExtra("partnerId", astroId)
                            putExtra("partnerName", astroName)
                            putExtra("partnerImage", astroImage)
                            putExtra("type", type)
                            val selectedPrice = when(type) {
                                "chat" -> chatPrice
                                "audio" -> callPrice
                                "video" -> videoPrice
                                else -> astroPrice
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
    val scrollState = rememberScrollState()
    val peacockTeal = Color(0xFFE87A1E)
    val yellowAccent = Color(0xFFFFD54F)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", color = CosmicAppTheme.colors.accent, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = CosmicAppTheme.colors.accent)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, "Share", tint = CosmicAppTheme.colors.accent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CosmicAppTheme.colors.bgStart)
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
                            label = "Chat",
                            color = CosmicAppTheme.colors.accent,
                            isEnabled = true,
                            onClick = { onAction("chat") }
                        )
                    }

                    if (isAudioOnline) {
                        ActionButton(
                            icon = Icons.Default.Call,
                            label = "Call",
                            color = CosmicAppTheme.colors.accent,
                            isEnabled = true,
                            onClick = { onAction("audio") }
                        )
                    }

                    if (isVideoOnline) {
                        ActionButton(
                            icon = androidx.compose.material.icons.Icons.Rounded.VideoCall,
                            label = "Video",
                            color = CosmicAppTheme.colors.accent,
                            isEnabled = true,
                            onClick = { onAction("video") }
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
