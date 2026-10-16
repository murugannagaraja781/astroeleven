package com.astroeleven.app.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: String = ""
    val currentOutputFile: String
        get() = outputFile
    var isRecording = false
        private set
    var startTime = 0L
        private set

    fun startRecording(): String? {
        val fileName = "voice_${System.currentTimeMillis()}.mp4"
        outputFile = "${context.cacheDir.absolutePath}/$fileName"

        try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            recorder = rec
            isRecording = true
            startTime = System.currentTimeMillis()
            return outputFile
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "startRecording() failed", e)
            try {
                recorder?.release()
            } catch (ignored: Exception) {}
            recorder = null
            isRecording = false
            val file = File(outputFile)
            if (file.exists()) {
                file.delete()
            }
            return null
        }
    }

    fun stopRecording(): Long {
        if (!isRecording) return 0L
        val duration = System.currentTimeMillis() - startTime
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "stop() failed", e)
            try { recorder?.release() } catch (ignored: Exception) {}
        } finally {
            recorder = null
            isRecording = false
        }
        return duration
    }

    fun cancelRecording() {
        if (!isRecording) return
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "cancel() failed", e)
            try { recorder?.release() } catch (ignored: Exception) {}
        } finally {
            recorder = null
            isRecording = false
        }
        val file = File(outputFile)
        if (file.exists()) {
            file.delete()
        }
    }
}
