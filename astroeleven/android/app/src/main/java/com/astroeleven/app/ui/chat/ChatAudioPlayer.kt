package com.astroeleven.app.ui.chat

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import android.widget.Toast
import android.os.Handler
import android.os.Looper

@OptIn(UnstableApi::class)
class ChatAudioPlayer(private val context: Context) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _bufferedProgress = MutableStateFlow(0f)
    val bufferedProgress: StateFlow<Float> = _bufferedProgress.asStateFlow()

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    private val _isPreparing = MutableStateFlow(false)
    val isPreparing: StateFlow<Boolean> = _isPreparing.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    companion object {
        @Volatile
        private var exoPlayer: ExoPlayer? = null

        @Volatile
        private var activeInstance: ChatAudioPlayer? = null

        @Synchronized
        private fun getPlayer(context: Context): ExoPlayer {
            if (exoPlayer == null) {
                val cache = VoiceMessageCache.getInstance(context)

                // Optimize OkHttpClient for media streaming
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                    .build()

                val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                    .setUserAgent("AstroElevenVoicePlayer/1.0")

                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                exoPlayer = ExoPlayer.Builder(context.applicationContext)
                    .setMediaSourceFactory(
                        DefaultMediaSourceFactory(context.applicationContext)
                            .setDataSourceFactory(cacheDataSourceFactory)
                    )
                    .build().apply {
                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build()
                        setAudioAttributes(audioAttributes, true /* handleAudioFocus */)

                        addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                activeInstance?.updatePlayingState(isPlaying)
                            }

                            override fun onPlaybackStateChanged(state: Int) {
                                when (state) {
                                    Player.STATE_BUFFERING -> {
                                        activeInstance?.updatePreparing(true)
                                    }
                                    Player.STATE_READY -> {
                                        activeInstance?.updatePreparing(false)
                                        activeInstance?.updateDuration(duration.toFloat())
                                    }
                                    Player.STATE_ENDED -> {
                                        activeInstance?.onPlaybackEnded()
                                    }
                                    Player.STATE_IDLE -> {
                                        activeInstance?.updatePlayingState(false)
                                        activeInstance?.updatePreparing(false)
                                    }
                                }
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                activeInstance?.onPlayerError(error)
                            }
                        })
                    }
            }
            return exoPlayer!!
        }
    }

    private var retryAttempted = false
    private var lastAttemptedUrl: String? = null

    fun play(url: String) {
        val player = getPlayer(context)

        if (_currentUrl.value == url && activeInstance == this) {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
            return
        }

        // Reset retry flags for a new audio file
        if (lastAttemptedUrl != url) {
            retryAttempted = false
        }
        lastAttemptedUrl = url

        // Deactivate the previous playing instance
        activeInstance?.deactivate()

        // Set this instance as active
        activeInstance = this
        _currentUrl.value = url
        _isPreparing.value = true
        _progress.value = 0f
        _bufferedProgress.value = 0f

        val cleanUrl = resolveAudioUrl(url)

        val mediaItem = MediaItem.fromUri(cleanUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        startProgressUpdates()
    }

    private fun resolveAudioUrl(url: String): String {
        if (url.isBlank()) return ""
        
        // 1. If it starts with http or https, return it directly after encoding spaces
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            return sanitizeUrl(url)
        }
        
        // 2. Check if it's an existing local file
        val localFile = java.io.File(url)
        if (localFile.exists() && localFile.isFile) {
            return "file://${localFile.absolutePath}"
        }
        
        // 3. Otherwise, prepend the server URL
        var baseUrl = com.astroeleven.app.utils.Constants.SERVER_URL
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }
        
        val relativePath = if (url.startsWith("/")) url else "/$url"
        val fullUrl = "$baseUrl$relativePath"
        
        return sanitizeUrl(fullUrl)
    }

    private fun sanitizeUrl(url: String): String {
        // Remove duplicate slashes except after protocol (http:// or https://)
        var cleaned = url.replace("(?<!https?:)/{2,}".toRegex(), "/")
        
        // Safely URL encode spaces and special characters
        try {
            val uri = java.net.URI(
                cleaned.substringBefore("://"),
                cleaned.substringAfter("://"),
                null
            )
            cleaned = uri.toASCIIString()
        } catch (e: Exception) {
            // Fallback space replace if URI parsing fails
            cleaned = cleaned.replace(" ", "%20")
        }
        return cleaned
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            val player = getPlayer(context)
            while (isActive) {
                if (activeInstance == this@ChatAudioPlayer) {
                    val current = player.currentPosition.toFloat()
                    val dur = player.duration.toFloat()
                    val buf = player.bufferedPosition.toFloat()
                    if (dur > 0) {
                        _progress.value = (current / dur).coerceIn(0f, 1f)
                        _bufferedProgress.value = (buf / dur).coerceIn(0f, 1f)
                    }
                }
                delay(50) // High-precision polling for smooth visual rendering
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun updatePlayingState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) {
            startProgressUpdates()
        } else {
            stopProgressUpdates()
        }
    }

    fun updatePreparing(isPreparing: Boolean) {
        _isPreparing.value = isPreparing
    }

    fun updateDuration(durationMs: Float) {
        _duration.value = durationMs
    }

    fun onPlaybackEnded() {
        _isPlaying.value = false
        _progress.value = 0f
        _bufferedProgress.value = 0f
        stopProgressUpdates()
    }

    fun onPlayerError(error: PlaybackException) {
        _isPreparing.value = false
        _isPlaying.value = false
        stopProgressUpdates()

        val cause = error.cause
        var userFriendlyMessage = "Voice playback failed"
        var isNetworkError = false

        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                val httpError = error as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
                val responseCode = httpError?.responseCode ?: 0
                userFriendlyMessage = when (responseCode) {
                    404 -> "Audio file not found on server (404)"
                    403 -> "Access denied (403)"
                    503 -> "Server busy, try again later (503)"
                    else -> "HTTP Server Error ($responseCode)"
                }
                isNetworkError = true
            }
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                userFriendlyMessage = "Network connection failed"
                isNetworkError = true
            }
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> {
                userFriendlyMessage = "Cleartext (HTTP) not permitted"
            }
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                userFriendlyMessage = "Local file not found"
            }
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> {
                userFriendlyMessage = "Storage permission denied"
            }
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                userFriendlyMessage = "Invalid or unsupported media format"
            }
            PlaybackException.ERROR_CODE_TIMEOUT -> {
                userFriendlyMessage = "Network timeout"
                isNetworkError = true
            }
            else -> {
                if (cause is java.security.cert.CertificateException || cause is javax.net.ssl.SSLException) {
                    userFriendlyMessage = "SSL certificate error"
                } else if (cause is java.io.IOException) {
                    userFriendlyMessage = "Network or file I/O error"
                    isNetworkError = true
                }
            }
        }

        // Automatic retry logic for temporary network errors
        if (isNetworkError && !retryAttempted && lastAttemptedUrl != null) {
            retryAttempted = true
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(context, "Network issue. Retrying...", Toast.LENGTH_SHORT).show()
                lastAttemptedUrl?.let { play(it) }
            }, 1500)
            return
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun deactivate() {
        stopProgressUpdates()
        _isPlaying.value = false
        _isPreparing.value = false
        _progress.value = 0f
        _bufferedProgress.value = 0f
    }

    fun seekTo(progress: Float) {
        if (activeInstance == this) {
            val player = getPlayer(context)
            val dur = player.duration
            if (dur > 0) {
                val targetPos = (dur * progress).toLong()
                player.seekTo(targetPos)
                _progress.value = progress
            }
        }
    }

    fun stop() {
        if (activeInstance == this) {
            val player = getPlayer(context)
            player.stop()
            player.clearMediaItems()
            deactivate()
            activeInstance = null
        }
    }

    fun release() {
        stop()
        coroutineScope.cancel()
    }
}
