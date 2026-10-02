package com.siddesh.smsalerts.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "safealert_preferences")

data class AppUserPreferences(
    val defaultEmergencyMessage: String = "Emergency alert: I may need assistance. Please contact me as soon as possible.",
    val locationSharingEnabled: Boolean = true,
    val countdownDurationSeconds: Int = 5,
    val requireHoldToConfirm: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val themeMode: String = "dark", // "dark", "light", "system"
    val directSmsEnabled: Boolean = true,
    val safetyCheckInDurationMinutes: Int = 30,
    val safetyCheckInActive: Boolean = false,
    val safetyCheckInTargetTimestamp: Long = 0L,
    val pendingEmailLinkEmail: String = "",
    val rememberEmail: Boolean = false,
    val savedEmail: String = "",
    // Advanced Emergency Trigger settings
    val emergencyButtonEnabled: Boolean = true,
    val shakeToAlertEnabled: Boolean = true,
    val requiredShakes: Int = 3,
    val shakeCooldownSeconds: Int = 30,
    val voiceAlertEnabled: Boolean = false,
    val emergencyCountdownSeconds: Int = 5,
    val requireConfirmation: Boolean = true,
    val allowSmsWithoutLocation: Boolean = true
)

class UserPreferencesDataStore(private val context: Context) {

    private object PreferenceKeys {
        val EMERGENCY_MESSAGE = stringPreferencesKey("emergency_message")
        val LOCATION_SHARING = booleanPreferencesKey("location_sharing")
        val COUNTDOWN_DURATION = intPreferencesKey("countdown_duration")
        val REQUIRE_HOLD = booleanPreferencesKey("require_hold")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DIRECT_SMS = booleanPreferencesKey("direct_sms")
        val CHECK_IN_DURATION = intPreferencesKey("check_in_duration")
        val CHECK_IN_ACTIVE = booleanPreferencesKey("check_in_active")
        val CHECK_IN_TARGET_TIME = longPreferencesKey("check_in_target_time")
        val PENDING_EMAIL_LINK_EMAIL = stringPreferencesKey("pending_email_link_email")
        val REMEMBER_EMAIL = booleanPreferencesKey("remember_email")
        val SAVED_EMAIL = stringPreferencesKey("saved_email")
        val EMERGENCY_BUTTON_ENABLED = booleanPreferencesKey("emergency_button_enabled")
        val SHAKE_TO_ALERT_ENABLED = booleanPreferencesKey("shake_to_alert_enabled")
        val REQUIRED_SHAKES = intPreferencesKey("required_shakes")
        val SHAKE_COOLDOWN_SECONDS = intPreferencesKey("shake_cooldown_seconds")
        val VOICE_ALERT_ENABLED = booleanPreferencesKey("voice_alert_enabled")
        val EMERGENCY_COUNTDOWN_SECONDS = intPreferencesKey("emergency_countdown_seconds")
        val REQUIRE_CONFIRMATION = booleanPreferencesKey("require_confirmation")
        val ALLOW_SMS_WITHOUT_LOCATION = booleanPreferencesKey("allow_sms_without_location")
    }

    val preferencesFlow: Flow<AppUserPreferences> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppUserPreferences(
                defaultEmergencyMessage = preferences[PreferenceKeys.EMERGENCY_MESSAGE]
                    ?: "Emergency alert: I may need assistance. Please contact me as soon as possible.",
                locationSharingEnabled = preferences[PreferenceKeys.LOCATION_SHARING] ?: true,
                countdownDurationSeconds = preferences[PreferenceKeys.COUNTDOWN_DURATION] ?: 5,
                requireHoldToConfirm = preferences[PreferenceKeys.REQUIRE_HOLD] ?: true,
                hapticFeedbackEnabled = preferences[PreferenceKeys.HAPTIC_FEEDBACK] ?: true,
                themeMode = preferences[PreferenceKeys.THEME_MODE] ?: "dark",
                directSmsEnabled = preferences[PreferenceKeys.DIRECT_SMS] ?: true,
                safetyCheckInDurationMinutes = preferences[PreferenceKeys.CHECK_IN_DURATION] ?: 30,
                safetyCheckInActive = preferences[PreferenceKeys.CHECK_IN_ACTIVE] ?: false,
                safetyCheckInTargetTimestamp = preferences[PreferenceKeys.CHECK_IN_TARGET_TIME] ?: 0L,
                pendingEmailLinkEmail = preferences[PreferenceKeys.PENDING_EMAIL_LINK_EMAIL] ?: "",
                rememberEmail = preferences[PreferenceKeys.REMEMBER_EMAIL] ?: false,
                savedEmail = preferences[PreferenceKeys.SAVED_EMAIL] ?: "",
                emergencyButtonEnabled = preferences[PreferenceKeys.EMERGENCY_BUTTON_ENABLED] ?: true,
                shakeToAlertEnabled = preferences[PreferenceKeys.SHAKE_TO_ALERT_ENABLED] ?: true,
                requiredShakes = preferences[PreferenceKeys.REQUIRED_SHAKES] ?: 3,
                shakeCooldownSeconds = preferences[PreferenceKeys.SHAKE_COOLDOWN_SECONDS] ?: 30,
                voiceAlertEnabled = preferences[PreferenceKeys.VOICE_ALERT_ENABLED] ?: false,
                emergencyCountdownSeconds = preferences[PreferenceKeys.EMERGENCY_COUNTDOWN_SECONDS] ?: 5,
                requireConfirmation = preferences[PreferenceKeys.REQUIRE_CONFIRMATION] ?: true,
                allowSmsWithoutLocation = preferences[PreferenceKeys.ALLOW_SMS_WITHOUT_LOCATION] ?: true
            )
        }

    suspend fun updateRememberEmail(remember: Boolean, email: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.REMEMBER_EMAIL] = remember
            if (remember) {
                preferences[PreferenceKeys.SAVED_EMAIL] = email.trim()
            } else {
                preferences.remove(PreferenceKeys.SAVED_EMAIL)
            }
        }
    }

    suspend fun updateEmergencyButtonEnabled(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.EMERGENCY_BUTTON_ENABLED] = enabled
        }
    }

    suspend fun updateShakeToAlert(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.SHAKE_TO_ALERT_ENABLED] = enabled
        }
    }

    suspend fun updateShakeToAlertEnabled(enabled: Boolean) = updateShakeToAlert(enabled)

    suspend fun updateRequiredShakes(shakes: Int) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.REQUIRED_SHAKES] = shakes
        }
    }

    suspend fun updateShakeCooldown(seconds: Int) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.SHAKE_COOLDOWN_SECONDS] = seconds
        }
    }

    suspend fun updateVoiceAlert(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.VOICE_ALERT_ENABLED] = enabled
        }
    }

    suspend fun updateRequireConfirmation(require: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.REQUIRE_CONFIRMATION] = require
        }
    }

    suspend fun updateAllowSmsWithoutLocation(allow: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.ALLOW_SMS_WITHOUT_LOCATION] = allow
        }
    }

    suspend fun updateEmergencyMessage(message: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.EMERGENCY_MESSAGE] = message
        }
    }

    suspend fun updateLocationSharing(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.LOCATION_SHARING] = enabled
        }
    }

    suspend fun updateCountdownDuration(seconds: Int) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.COUNTDOWN_DURATION] = seconds
        }
    }

    suspend fun updateRequireHold(requireHold: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.REQUIRE_HOLD] = requireHold
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = themeMode
        }
    }

    suspend fun updateDirectSms(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.DIRECT_SMS] = enabled
        }
    }

    suspend fun startSafetyCheckIn(durationMinutes: Int, targetTimestamp: Long) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.CHECK_IN_DURATION] = durationMinutes
            preferences[PreferenceKeys.CHECK_IN_ACTIVE] = true
            preferences[PreferenceKeys.CHECK_IN_TARGET_TIME] = targetTimestamp
        }
    }

    suspend fun cancelOrCompleteSafetyCheckIn() {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.CHECK_IN_ACTIVE] = false
            preferences[PreferenceKeys.CHECK_IN_TARGET_TIME] = 0L
        }
    }

    suspend fun savePendingEmailForLink(email: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.PENDING_EMAIL_LINK_EMAIL] = email
        }
    }

    suspend fun clearPendingEmailForLink() {
        context.userDataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.PENDING_EMAIL_LINK_EMAIL)
        }
    }
}
