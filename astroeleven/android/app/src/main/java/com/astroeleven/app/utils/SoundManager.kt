package com.astroeleven.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build

object SoundManager {

    private var toneGen: ToneGenerator? = null

    private fun getTone(): ToneGenerator? {
        if (toneGen == null) {
            try {
                toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
            } catch (e: Exception) {
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
                } catch (e2: Exception) {}
            }
        }
        return toneGen
    }

    // Outgoing Sent sound: crisp "tak / ting"
    fun playSentSound() {
        try {
            getTone()?.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Incoming Received sound: pleasant alert "ting"
    fun playReceiveSound() {
        try {
            getTone()?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Read receipt sound: soft double click / read confirmation
    fun playReadReceiptSound() {
        try {
            getTone()?.startTone(ToneGenerator.TONE_PROP_BEEP2, 35)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun playEndChatSound() {
        try {
            getTone()?.startTone(ToneGenerator.TONE_PROP_NACK, 120)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun playAcceptSound() {
        try {
            ToneGenerator(AudioManager.STREAM_RING, 80).startTone(ToneGenerator.TONE_SUP_CONFIRM, 150)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun playRejectSound() {
        try {
            ToneGenerator(AudioManager.STREAM_RING, 80).startTone(ToneGenerator.TONE_SUP_ERROR, 150)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
