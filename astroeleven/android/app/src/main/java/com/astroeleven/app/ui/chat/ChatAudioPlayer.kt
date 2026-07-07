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

        // Deactivate the previous playing instance
        activeInstance?.deactivate()

        // Set this instance as active
        activeInstance = this
        _currentUrl.value = url
        _isPreparing.value = true
        _progress.value = 0f
        _bufferedProgress.value = 0f

        val cleanUrl = if (url.startsWith("http")) {
            url
        } else {
            if (url.startsWith("/")) "file://$url" else url
        }

        val mediaItem = MediaItem.fromUri(cleanUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        startProgressUpdates()
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
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Voice playback failed: ${error.message}", Toast.LENGTH_SHORT).show()
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
