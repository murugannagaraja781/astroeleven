package com.astroeleven.app.ui.academy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astroeleven.app.data.api.ApiClient
import com.astroeleven.app.ui.theme.CosmicAppTheme
import kotlinx.coroutines.launch
import org.json.JSONObject

class AcademyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra("target") ?: "user"
        setContent {
            CosmicAppTheme {
                AcademyScreen(target = target, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyScreen(target: String = "user", onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val isAstro = target.equals("astrologer", ignoreCase = true)

    fun loadVideos() {
        isLoading = true
        scope.launch {
            try {
                val response = ApiClient.api.getAcademyVideos(target = target)
                if (response.isSuccessful && response.body() != null) {
                    val root = JSONObject(response.body().toString())
                    val arr = root.optJSONArray("videos")
                    if (arr != null && arr.length() > 0) {
                        val list = mutableListOf<VideoItem>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(VideoItem(
                                id = obj.optString("_id", ""),
                                title = obj.optString("title", "Video"),
                                url = obj.optString("youtubeUrl", ""),
                                category = obj.optString("category", "General"),
                                thumbnail = obj.optString("thumbnail", "")
                            ))
                        }
                        videos = list
                    } else {
                        videos = emptyList()
                    }
                } else {
                    videos = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(target) {
        loadVideos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val mainTitle = if (isAstro) "🎓 Astrologer Learning Hub" else "🎓 Astro Academy"
                        val subTitle = if (isAstro) "ஜோதிடர்களுக்கான கற்றல் & வழிகாட்டிகள்" else "ஜோதிட அடிப்படைகள் & பயிற்சிகள்"
                        Text(mainTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(subTitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { loadVideos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isAstro) Color(0xFF6200EE) else Color(0xFFD32F2F)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F8FA))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = if (isAstro) Color(0xFF6200EE) else Color(0xFFD32F2F)
                )
            } else if (videos.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val emptyMsg = if (isAstro) 
                            "Super Admin விரைவில் ஜோதிடர்களுக்கான புதிய பயிற்சி வீடியோ லிங்க்களை பதிவேற்றுவார்." 
                            else "Super Admin விரைவில் புதிய வீடியோ லிங்க்களை பதிவேற்றுவார்."
                        Text(
                            emptyMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadVideos() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAstro) Color(0xFF6200EE) else Color(0xFFD32F2F)
                            )
                        ) {
                            Text("Refresh")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(videos) { video ->
                        VideoCard(video = video, isAstro = isAstro)
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCard(video: VideoItem, isAstro: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val accentColor = if (isAstro) Color(0xFF6200EE) else Color(0xFFD32F2F)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open video link: ${video.url}", Toast.LENGTH_SHORT).show()
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E1E1E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            video.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    video.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open link: ${video.url}", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Video",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class VideoItem(
    val id: String = "",
    val title: String,
    val url: String,
    val category: String,
    val thumbnail: String = ""
)
