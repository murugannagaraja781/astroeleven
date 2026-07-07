package com.astroeleven.app.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioPlayerBubble(audioUrl: String, durationStr: String, isMe: Boolean, audioPlayer: ChatAudioPlayer) {
    val isPlayingGlobal by audioPlayer.isPlaying.collectAsState()
    val currentUrlGlobal by audioPlayer.currentUrl.collectAsState()
    val progressGlobal by audioPlayer.progress.collectAsState()
    val bufferedProgressGlobal by audioPlayer.bufferedProgress.collectAsState()
    val isPreparingGlobal by audioPlayer.isPreparing.collectAsState()
    val durationGlobal by audioPlayer.duration.collectAsState()

    val isThisPlaying = isPlayingGlobal && currentUrlGlobal == audioUrl
    val isThisPreparing = isPreparingGlobal && currentUrlGlobal == audioUrl

    val currentProgress = if (currentUrlGlobal == audioUrl) progressGlobal else 0f
    val currentBufferedProgress = if (currentUrlGlobal == audioUrl) bufferedProgressGlobal else 0f
    val currentDuration = if (currentUrlGlobal == audioUrl) durationGlobal else 0f

    val currentPosition = currentProgress * currentDuration

    // Zero-latency local drag seeking state
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = dragProgress ?: currentProgress

    // WhatsApp premium colors
    val playBtnColor = if (isMe) Color(0xFF25D366) else Color(0xFF34B7F1) // Green vs Blue
    val waveActiveColor = if (isMe) Color(0xFF1E88E5) else Color(0xFF25D366) // Darker highlight color
    val waveBufferedColor = Color.White.copy(alpha = 0.45f)
    val waveInactiveColor = Color.White.copy(alpha = 0.2f)
    val textColor = Color.White.copy(alpha = 0.7f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        // Play / Pause / Buffering button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clickable {
                    if (isThisPreparing) return@clickable
                    audioPlayer.play(audioUrl)
                }
        ) {
            if (isThisPreparing) {
                CircularProgressIndicator(
                    color = playBtnColor,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isThisPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Custom Waveform and Timers
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val progress = (offset.x / size.width).coerceIn(0f, 1f)
                            audioPlayer.seekTo(progress)
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                dragProgress?.let { audioPlayer.seekTo(it) }
                                dragProgress = null
                            },
                            onDragCancel = {
                                dragProgress = null
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                                dragProgress = progress
                            }
                        )
                    }
            ) {
                val barCount = 32
                val barWidth = 3.dp.toPx()
                val spacing = 2.dp.toPx()
                
                // Heights list representing varying recorded levels
                val heights = listOf(
                    10, 14, 18, 12, 16, 24, 20, 14, 
                    10, 16, 22, 12, 18, 15, 26, 12,
                    14, 20, 10, 16, 22, 14, 18, 12,
                    16, 24, 10, 14, 18, 12, 16, 10
                )

                val totalWidthNeeded = barCount * barWidth + (barCount - 1) * spacing
                val startOffset = (size.width - totalWidthNeeded) / 2f

                for (i in 0 until barCount) {
                    val rawHeight = heights[i % heights.size].dp.toPx()
                    val x = startOffset + i * (barWidth + spacing)
                    val y = (size.height - rawHeight) / 2f

                    val barProgress = i.toFloat() / barCount
                    val color = when {
                        barProgress <= displayProgress -> waveActiveColor
                        barProgress <= currentBufferedProgress -> waveBufferedColor
                        else -> waveInactiveColor
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, rawHeight),
                        cornerRadius = CornerRadius(1.5.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentSec = (currentPosition / 1000).toInt()
                val currentStr = String.format("%02d:%02d", currentSec / 60, currentSec % 60)

                Text(
                    text = if (isThisPlaying || displayProgress > 0) currentStr else durationStr,
                    fontSize = 11.sp,
                    color = textColor
                )
            }
        }
    }
}
