package com.astroeleven.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.astroeleven.app.ui.chat.ChatActivity

/**
 * Receiver for Accept / Reject actions from the full‑screen chat notification.
 *
 * ACTION_ACCEPT_CHAT  – launches ChatActivity (same extras as the full‑screen intent).
 * ACTION_REJECT_CHAT  – cancels the notification and optionally notifies the backend.
 */
class ChatActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ACCEPT_CHAT = "com.astroeleven.ACTION_ACCEPT_CHAT"
        const val ACTION_REJECT_CHAT = "com.astroeleven.ACTION_REJECT_CHAT"
        private const val TAG = "ChatActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val callerId = intent.getStringExtra("callerId") ?: return
        val sessionId = intent.getStringExtra("sessionId") ?: return
        val callerName = intent.getStringExtra("callerName") ?: "Client"
        val birthDataStr = intent.getStringExtra("birthData")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_ACCEPT_CHAT -> {
                Log.d(TAG, "User accepted chat request from $callerId")
                notificationManager.cancel(sessionId.hashCode())
                notificationManager.cancel(callerId.hashCode())

                val chatIntent = Intent(context, ChatActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("toUserId", callerId)
                    putExtra("toUserName", callerName)
                    putExtra("sessionId", sessionId)
                    putExtra("isNewRequest", true)
                    if (birthDataStr != null) {
                        putExtra("birthData", birthDataStr)
                    }
                }
                context.startActivity(chatIntent)
            }
            ACTION_REJECT_CHAT -> {
                Log.d(TAG, "User rejected chat request from $callerId")
                notificationManager.cancel(sessionId.hashCode())
                notificationManager.cancel(callerId.hashCode())

                try {
                    val payload = org.json.JSONObject().apply {
                        put("sessionId", sessionId)
                        put("toUserId", callerId)
                        put("type", "chat")
                        put("accept", false)
                    }
                    com.astroeleven.app.data.remote.SocketManager.init()
                    com.astroeleven.app.data.remote.SocketManager.getSocket()?.emit("answer-session-native", payload)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to emit rejection to backend", e)
                }
            }
            else -> {
                Log.w(TAG, "Unknown action received: $action")
            }
        }
    }
}
