package com.astroeleven.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.astroeleven.app.data.model.AuthResponse
import com.google.gson.Gson

class TokenManager(private val context: Context) {

    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "secure_prefs"
        private const val BACKUP_PREFS_NAME = "astro_prefs_backup"
    }

    private val backupPreferences: android.content.SharedPreferences by lazy {
        context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val sharedPreferences: android.content.SharedPreferences = initPreferences(context)

    private fun initPreferences(ctx: Context): android.content.SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                ctx,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "EncryptedSharedPreferences failed, attempting safe recovery", e)
            try {
                // Try recovery with a recreated MasterKey
                val masterKey = MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    ctx,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                android.util.Log.e(TAG, "EncryptedSharedPreferences recovery failed, using safe backup SharedPreferences", e2)
                ctx.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    fun saveUserSession(auth: AuthResponse) {
        val json = Gson().toJson(auth)
        try {
            sharedPreferences.edit().putString("user_session", json).apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed writing to sharedPreferences", e)
        }
        try {
            backupPreferences.edit().putString("user_session", json).apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed writing to backupPreferences", e)
        }
    }

    fun getUserSession(): AuthResponse? {
        val json = try {
            sharedPreferences.getString("user_session", null)
        } catch (e: Exception) {
            null
        } ?: try {
            backupPreferences.getString("user_session", null)
        } catch (e: Exception) {
            null
        } ?: return null

        return try {
            Gson().fromJson(json, AuthResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession() {
        try {
            sharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed clearing sharedPreferences", e)
        }
        try {
            backupPreferences.edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed clearing backupPreferences", e)
        }
    }

    fun isLoggedIn(): Boolean {
        return getUserSession() != null
    }

    fun updateWalletBalance(balance: Double) {
        val session = getUserSession() ?: return
        val updated = session.copy(walletBalance = balance)
        saveUserSession(updated)
    }

    fun updateSuperWalletBalance(balance: Double) {
        val session = getUserSession() ?: return
        val updated = session.copy(superWalletBalance = balance)
        saveUserSession(updated)
    }

    // Daily Progress Management
    fun getDailyProgress(): Int {
        return sharedPreferences.getInt("daily_progress", 0)
    }

    fun setDailyProgress(value: Int) {
        sharedPreferences.edit().putInt("daily_progress", value).apply()
    }

    fun getLastDate(): String {
        return sharedPreferences.getString("last_progress_date", "") ?: ""
    }

    fun setLastDate(date: String) {
        sharedPreferences.edit().putString("last_progress_date", date).apply()
    }

    // Pending Referral Code Management
    fun savePendingReferral(code: String) {
        if (code.isNotBlank()) {
            sharedPreferences.edit().putString("pending_referral", code.uppercase()).apply()
        }
    }

    fun getPendingReferral(): String? {
        return sharedPreferences.getString("pending_referral", null)
    }

    fun clearPendingReferral() {
        sharedPreferences.edit().remove("pending_referral").apply()
    }

    // App Version Tracking (for auto-logout on update)
    fun getLastVersionCode(): Long {
        return sharedPreferences.getLong("last_version_code", 0L)
    }

    fun saveLastVersionCode(version: Long) {
        sharedPreferences.edit().putLong("last_version_code", version).apply()
    }
}
