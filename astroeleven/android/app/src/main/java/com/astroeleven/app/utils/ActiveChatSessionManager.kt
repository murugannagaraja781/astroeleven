package com.astroeleven.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.astroeleven.app.R
import com.astroeleven.app.ui.chat.ChatActivity
import com.astroeleven.app.ui.call.CallActivity

/**
 * ActiveChatSessionManager - Persists active chat and call state across app switches,
 * process backgrounding, and provides ongoing notifications and quick re-entry.
 */
object ActiveChatSessionManager {

    private const val PREFS_NAME = "astro_active_chat_prefs"
    private const val KEY_SESSION_ID = "active_session_id"
    private const val KEY_PARTNER_ID = "active_partner_id"
    private const val KEY_PARTNER_NAME = "active_partner_name"
    private const val KEY_IS_ASTRO = "active_is_astro"
    private const val KEY_START_TIME = "active_start_time"
    private const val KEY_SESSION_TYPE = "active_session_type" // "chat", "audio", "video"

    const val CHAT_NOTIFICATION_ID = 8888
    const val NOTIFICATION_CHANNEL_ID = "active_chat_channel"

    data class ActiveSessionInfo(
        val sessionId: String,
        val toUserId: String,
        val toUserName: String,
        val isAstrologer: Boolean,
        val startTime: Long,
        val sessionType: String = "chat"
    )

    fun setActiveSession(
        context: Context,
        sessionId: String,
        toUserId: String,
        toUserName: String,
        isAstrologer: Boolean,
        sessionType: String = "chat"
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putString(KEY_PARTNER_ID, toUserId)
            .putString(KEY_PARTNER_NAME, toUserName)
            .putBoolean(KEY_IS_ASTRO, isAstrologer)
            .putLong(KEY_START_TIME, System.currentTimeMillis())
            .putString(KEY_SESSION_TYPE, sessionType)
            .apply()

        CallState.isCallActive = true
        CallState.currentSessionId = sessionId

        showOngoingChatNotification(context, sessionId, toUserId, toUserName, sessionType)
    }

    fun getActiveSession(context: Context): ActiveSessionInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionId = prefs.getString(KEY_SESSION_ID, null) ?: return null
        val toUserId = prefs.getString(KEY_PARTNER_ID, "") ?: ""
        val toUserName = prefs.getString(KEY_PARTNER_NAME, "Partner") ?: "Partner"
        val isAstrologer = prefs.getBoolean(KEY_IS_ASTRO, false)
        val startTime = prefs.getLong(KEY_START_TIME, System.currentTimeMillis())
        val sessionType = prefs.getString(KEY_SESSION_TYPE, "chat") ?: "chat"

        return ActiveSessionInfo(
            sessionId = sessionId,
            toUserId = toUserId,
            toUserName = toUserName,
            isAstrologer = isAstrologer,
            startTime = startTime,
            sessionType = sessionType
        )
    }

    fun clearActiveSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        CallState.isCallActive = false
        CallState.currentSessionId = null

        dismissNotification(context)
    }

    fun hasActiveSession(context: Context): Boolean {
        return getActiveSession(context) != null
    }

    fun showOngoingChatNotification(
        context: Context,
        sessionId: String,
        toUserId: String,
        partnerName: String,
        sessionType: String = "chat"
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Active Session Notification",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Displays ongoing chat or call notification to easily return to active session"
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }

            val isCall = (sessionType == "audio" || sessionType == "video")
            val targetClass = if (isCall) CallActivity::class.java else ChatActivity::class.java

            val resumeIntent = Intent(context, targetClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("sessionId", sessionId)
                putExtra("toUserId", toUserId)
                putExtra("partnerId", toUserId)
                putExtra("toUserName", partnerName)
                putExtra("partnerName", partnerName)
                putExtra("type", sessionType)
                putExtra("callType", sessionType)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                CHAT_NOTIFICATION_ID,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (isCall) {
                "Astro Eleven: $partnerName உடன் அழைப்பு தொடர்கிறது"
            } else {
                "Astro Eleven: $partnerName உடன் சாட் நடக்கிறது"
            }

            val contentText = if (isCall) {
                "அழைப்பு திரைக்கு மீண்டும் செல்ல தட்டவும் (Tap to return to call)"
            } else {
                "சாட் பாக்ஸிற்கு மீண்டும் செல்ல தட்டவும் (Tap to return to chat)"
            }

            val actionLabel = if (isCall) {
                "அழைப்புக்கு திரும்பு (Return to Call)"
            } else {
                "சாட்டுக்கு திரும்பு (Open Chat)"
            }

            val actionIcon = if (isCall) android.R.drawable.ic_menu_call else android.R.drawable.ic_menu_send

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(if (isCall) NotificationCompat.CATEGORY_CALL else NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                    actionIcon,
                    actionLabel,
                    pendingIntent
                )
                .build()

            nm.notify(CHAT_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissNotification(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(CHAT_NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
