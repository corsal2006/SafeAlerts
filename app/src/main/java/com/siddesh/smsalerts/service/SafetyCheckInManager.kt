package com.siddesh.smsalerts.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.siddesh.smsalerts.data.preferences.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SafetyCheckInState(
    val isActive: Boolean = false,
    val durationMinutes: Int = 30,
    val targetTimeMillis: Long = 0L,
    val remainingSeconds: Long = 0L,
    val isExpired: Boolean = false
)

class SafetyCheckInManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val preferences = UserPreferencesDataStore(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _checkInState = MutableStateFlow(SafetyCheckInState())
    val checkInState: StateFlow<SafetyCheckInState> = _checkInState.asStateFlow()

    init {
        scope.launch {
            preferences.preferencesFlow.collect { prefs ->
                val now = System.currentTimeMillis()
                if (prefs.safetyCheckInActive && prefs.safetyCheckInTargetTimestamp > 0) {
                    val remaining = ((prefs.safetyCheckInTargetTimestamp - now) / 1000).coerceAtLeast(0)
                    _checkInState.value = SafetyCheckInState(
                        isActive = true,
                        durationMinutes = prefs.safetyCheckInDurationMinutes,
                        targetTimeMillis = prefs.safetyCheckInTargetTimestamp,
                        remainingSeconds = remaining,
                        isExpired = remaining == 0L
                    )
                } else {
                    _checkInState.value = SafetyCheckInState(
                        isActive = false,
                        durationMinutes = prefs.safetyCheckInDurationMinutes,
                        targetTimeMillis = 0L,
                        remainingSeconds = 0L,
                        isExpired = false
                    )
                }
            }
        }
    }

    fun startCheckIn(durationMinutes: Int) {
        val targetMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)

        scope.launch {
            preferences.startSafetyCheckIn(durationMinutes, targetMillis)
        }

        val intent = Intent(context, SafetyCheckInReceiver::class.java).apply {
            action = SafetyCheckInReceiver.ACTION_CHECK_IN_EXPIRED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.let { am ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
                    }
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            }
        }
    }

    fun markSafeOrCancel() {
        val intent = Intent(context, SafetyCheckInReceiver::class.java).apply {
            action = SafetyCheckInReceiver.ACTION_CHECK_IN_EXPIRED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
        }

        scope.launch {
            preferences.cancelOrCompleteSafetyCheckIn()
        }
    }

    fun updateRemainingTime() {
        val current = _checkInState.value
        if (current.isActive && current.targetTimeMillis > 0) {
            val now = System.currentTimeMillis()
            val remaining = ((current.targetTimeMillis - now) / 1000).coerceAtLeast(0)
            _checkInState.value = current.copy(
                remainingSeconds = remaining,
                isExpired = remaining == 0L
            )
        }
    }
}
