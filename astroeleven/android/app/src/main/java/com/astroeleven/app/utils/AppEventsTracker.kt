package com.astroeleven.app.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import java.math.BigDecimal
import java.util.Currency

object AppEventsTracker {
    private const val TAG = "AppEventsTracker"

    /**
     * Track App Activation / Launch
     */
    fun trackAppLaunch(context: Context) {
        try {
            val logger = AppEventsLogger.newLogger(context)
            logger.logEvent("app_launch")
            Log.d(TAG, "[Event] App Launch Tracked")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking app launch", e)
        }
    }

    /**
     * Track User Registration (First time signup)
     */
    fun trackRegistration(context: Context, method: String = "phone_otp", userId: String? = null) {
        try {
            val logger = AppEventsLogger.newLogger(context)
            val params = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_REGISTRATION_METHOD, method)
                userId?.let { putString("user_id", it) }
            }
            logger.logEvent(AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION, params)
            Log.d(TAG, "[Event] Registration Tracked: userId=$userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking registration", e)
        }
    }

    /**
     * Track User Login
     */
    fun trackLogin(context: Context, method: String = "phone_otp", userId: String? = null, role: String? = null) {
        try {
            val logger = AppEventsLogger.newLogger(context)
            val params = Bundle().apply {
                putString("login_method", method)
                userId?.let { putString("user_id", it) }
                role?.let { putString("role", it) }
            }
            logger.logEvent("fb_mobile_login", params)
            Log.d(TAG, "[Event] Login Tracked: userId=$userId, role=$role")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking login", e)
        }
    }

    /**
     * Track Wallet Recharge / Purchase Event
     */
    fun trackPurchase(context: Context, amount: Double, orderId: String? = null, currency: String = "INR") {
        try {
            val logger = AppEventsLogger.newLogger(context)
            val params = Bundle().apply {
                orderId?.let { putString("order_id", it) }
                putDouble("amount", amount)
                putString("currency", currency)
            }
            logger.logPurchase(BigDecimal.valueOf(amount), Currency.getInstance(currency), params)
            Log.d(TAG, "[Event] Purchase Tracked: amount=$amount, orderId=$orderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking purchase", e)
        }
    }

    /**
     * Track Chat Consultation Started
     */
    fun trackChatStarted(context: Context, astroId: String?, astroName: String?, ratePerMin: Double? = 0.0) {
        try {
            val logger = AppEventsLogger.newLogger(context)
            val params = Bundle().apply {
                putString("astrologer_id", astroId ?: "")
                putString("astrologer_name", astroName ?: "")
                ratePerMin?.let { putDouble("rate_per_minute", it) }
            }
            logger.logEvent("chat_session_started", params)
            Log.d(TAG, "[Event] Chat Started Tracked: astro=$astroName")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking chat started", e)
        }
    }

    /**
     * Track Call Consultation Started (Audio / Video)
     */
    fun trackCallStarted(context: Context, astroId: String?, astroName: String?, callType: String = "audio") {
        try {
            val logger = AppEventsLogger.newLogger(context)
            val params = Bundle().apply {
                putString("astrologer_id", astroId ?: "")
                putString("astrologer_name", astroName ?: "")
                putString("call_type", callType)
            }
            logger.logEvent("call_session_started", params)
            Log.d(TAG, "[Event] Call Started Tracked: type=$callType, astro=$astroName")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking call started", e)
        }
    }

    /**
     * Track Important Content View (Horoscope, Puja, Astrologer Profile)
     */
    fun trackViewContent(context: Context, contentType: String, contentId: String, contentName: String? = null) {
        try {
            val logger = AppEventsLogger.newLogger(context)
            val params = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, contentType)
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, contentId)
                contentName?.let { putString("content_name", it) }
            }
            logger.logEvent(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT, params)
            Log.d(TAG, "[Event] View Content Tracked: type=$contentType, id=$contentId")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking view content", e)
        }
    }
}
