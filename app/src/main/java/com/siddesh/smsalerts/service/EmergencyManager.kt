package com.siddesh.smsalerts.service

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.siddesh.smsalerts.data.location.LocationResult
import com.siddesh.smsalerts.data.location.LocationService
import com.siddesh.smsalerts.data.model.EmergencyAlert
import com.siddesh.smsalerts.data.model.EmergencyContact
import com.siddesh.smsalerts.data.preferences.UserPreferencesDataStore
import com.siddesh.smsalerts.data.repository.AlertsRepository
import com.siddesh.smsalerts.data.repository.AuthRepository
import com.siddesh.smsalerts.data.repository.ContactsRepository
import com.siddesh.smsalerts.data.repository.UserRepository
import com.siddesh.smsalerts.data.sms.SmsDispatchResult
import com.siddesh.smsalerts.data.sms.SmsSender
import com.siddesh.smsalerts.ui.viewmodel.SosStage
import com.siddesh.smsalerts.utils.DateUtils
import com.siddesh.smsalerts.utils.HapticUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "SafeAlerts/Emergency"

/**
 * Centralized Emergency Dispatch Engine.
 * All triggers (Emergency Button, Shortcuts [Medical, Accident, Fire, Security],
 * Shake 3x, Voice "HELP", Foreground Service) route through this single engine.
 *
 * Guarantees:
 * - Single source of truth for alert generation, SMS transmission, and Firestore audit.
 * - Deduplication: rejects simultaneous triggers while an alert is processing.
 * - Sensor debouncing & post-trigger cooldown protection.
 * - Offline resilience: SMS sends first; Firestore database errors never block emergency SMS.
 */
class EmergencyManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: EmergencyManager? = null

        fun getInstance(context: Context): EmergencyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmergencyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository.getInstance()
    private val contactsRepository = ContactsRepository()
    private val alertsRepository = AlertsRepository()
    private val locationService = LocationService(context)
    private val smsSender = SmsSender(context)
    private val preferences = UserPreferencesDataStore(context)

    private val _stage = MutableStateFlow<SosStage>(SosStage.Countdown(5))
    val stage: StateFlow<SosStage> = _stage.asStateFlow()

    private var countdownJob: Job? = null
    private var dispatchJob: Job? = null

    @Volatile
    var isDispatching: Boolean = false
        private set

    private var lastTriggerTimestamp = 0L

    fun startCountdown(
        triggerSource: String = "BUTTON",
        emergencyType: String = "PERSONAL",
        customMessage: String = "",
        countdownSeconds: Int = 5
    ) {
        if (isDispatching) {
            Log.w(TAG, "EMERGENCY_REJECTED: Alert is already being processed.")
            _stage.value = SosStage.Error(
                reason = "Emergency alert is already being processed.",
                canRetry = false
            )
            return
        }

        countdownJob?.cancel()
        dispatchJob?.cancel()

        if (countdownSeconds <= 0) {
            dispatchJob = scope.launch {
                executeDispatch(
                    triggerSource = triggerSource,
                    emergencyType = emergencyType,
                    customMessage = customMessage
                )
            }
            return
        }

        countdownJob = scope.launch {
            for (sec in countdownSeconds downTo 1) {
                _stage.value = SosStage.Countdown(sec)
                HapticUtils.vibrateCountdownTick(context)
                delay(1000L)
            }
            executeDispatch(
                triggerSource = triggerSource,
                emergencyType = emergencyType,
                customMessage = customMessage
            )
        }
    }

    fun triggerAutomaticEmergency(source: String) {
        Log.i(TAG, "AUTOMATIC_EMERGENCY_INVOKED: source=$source")
        triggerEmergency(
            triggerSource = source,
            emergencyType = "PERSONAL EMERGENCY",
            customMessage = "I need immediate assistance.",
            bypassCountdown = true
        )
    }

    fun triggerShakeEmergency() = triggerAutomaticEmergency("SHAKE")

    fun triggerVoiceEmergency() = triggerAutomaticEmergency("VOICE")

    fun triggerManualEmergency(type: String) {
        startCountdown(
            triggerSource = "BUTTON",
            emergencyType = type,
            customMessage = "",
            countdownSeconds = 5
        )
    }

    fun triggerEmergency(
        triggerSource: String,
        emergencyType: String = "PERSONAL",
        customMessage: String = "",
        bypassCountdown: Boolean = false
    ) {
        val now = System.currentTimeMillis()

        if (isDispatching) {
            Log.w(TAG, "EMERGENCY_REJECTED: Alert dispatch in progress.")
            _stage.value = SosStage.Error(
                reason = "Emergency alert is already being processed.",
                canRetry = false
            )
            return
        }

        // Enforce cooldown (minimum 30s to prevent accidental repeat triggers)
        val cooldownMs = 30_000L
        if (lastTriggerTimestamp > 0L && now - lastTriggerTimestamp < cooldownMs) {
            val remainingSec = ((lastTriggerTimestamp + cooldownMs - now) / 1000).coerceAtLeast(1)
            Log.w(TAG, "EMERGENCY_COOLDOWN_ACTIVE: ${remainingSec}s remaining.")
            _stage.value = SosStage.Error(
                reason = "Cooldown active. Please wait $remainingSec seconds before triggering another emergency.",
                canRetry = false
            )
            return
        }

        countdownJob?.cancel()

        if (bypassCountdown) {
            scope.launch {
                executeDispatch(
                    triggerSource = triggerSource,
                    emergencyType = emergencyType,
                    customMessage = customMessage
                )
            }
        } else {
            startCountdown(
                triggerSource = triggerSource,
                emergencyType = emergencyType,
                customMessage = customMessage
            )
        }
    }

    fun cancelEmergency() {
        countdownJob?.cancel()
        dispatchJob?.cancel()
        isDispatching = false
        _stage.value = SosStage.Cancelled
        HapticUtils.vibrateTick(context)
        Log.i(TAG, "EMERGENCY_CANCELLED by user.")
    }

    private suspend fun executeDispatch(
        triggerSource: String,
        emergencyType: String,
        customMessage: String
    ) {
        if (isDispatching) return
        isDispatching = true
        lastTriggerTimestamp = System.currentTimeMillis()

        Log.i(TAG, "EMERGENCY_TRIGGERED source=$triggerSource, type=$emergencyType")

        try {
            val userId = authRepository.currentUserId
            if (userId == null) {
                _stage.value = SosStage.Error(
                    reason = "User session expired. Please sign in to authenticate your emergency alerts.",
                    canRetry = false
                )
                Log.e(TAG, "AUTH_FAILURE: User not authenticated.")
                return
            }

            // 1. Fetch enabled trusted contacts
            val contactsList: List<EmergencyContact> = try {
                contactsRepository.getContactsFlow(userId).first().filter { it.isActive && it.enabled }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching contacts: ${e.message}")
                emptyList()
            }

            if (contactsList.isEmpty()) {
                Log.e(TAG, "NO_TRUSTED_CONTACTS: Emergency triggered ($triggerSource) but zero active contacts found.")
                postNotification(
                    title = "⚠️ Safe Alerts: No Contacts",
                    message = "Emergency alert triggered by $triggerSource, but no trusted contacts are registered."
                )
                _stage.value = SosStage.Error(
                    reason = "No active emergency contacts configured! Please add at least one trusted contact in the Contacts tab.",
                    canRetry = false
                )
                HapticUtils.vibrateError(context)
                return
            }

            // 2. Verify SEND_SMS permission upfront
            if (!smsSender.hasSmsPermission()) {
                Log.w(TAG, "EMERGENCY_HALTED: SEND_SMS permission is not granted")
                _stage.value = SosStage.Error(
                    reason = "SMS permission (SEND_SMS) is required to automatically send emergency alerts. Please grant SMS permission in App Settings.",
                    canRetry = true,
                    fallbackIntent = com.siddesh.smsalerts.utils.PermissionManager.createAppSettingsIntent(context)
                )
                HapticUtils.vibrateError(context)
                return
            }

            // 3. Acquire current GPS location (waiting for valid location, never fake/null)
            _stage.value = SosStage.AcquiringLocation
            var latitude: Double? = null
            var longitude: Double? = null
            var mapsUrl: String? = null

            when (val locResult = locationService.getCurrentLocation(timeoutMs = 10000L)) {
                is LocationResult.Success -> {
                    latitude = locResult.latitude
                    longitude = locResult.longitude
                    mapsUrl = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                    Log.d(TAG, "LOCATION_SUCCESS: coordinates acquired successfully")
                }
                is LocationResult.GpsDisabled -> {
                    Log.w(TAG, "EMERGENCY_HALTED: GPS is disabled on device")
                    _stage.value = SosStage.Error(
                        reason = "Location/GPS is disabled on your device. Please turn on Location in Settings so your emergency coordinates can be attached.",
                        canRetry = true,
                        fallbackIntent = com.siddesh.smsalerts.utils.PermissionManager.createLocationSettingsIntent()
                    )
                    HapticUtils.vibrateError(context)
                    return
                }
                is LocationResult.PermissionDenied -> {
                    Log.w(TAG, "EMERGENCY_HALTED: Location permission denied")
                    _stage.value = SosStage.Error(
                        reason = "Location permission is required to acquire your emergency coordinates. Please grant location permission in App Settings.",
                        canRetry = true,
                        fallbackIntent = com.siddesh.smsalerts.utils.PermissionManager.createAppSettingsIntent(context)
                    )
                    HapticUtils.vibrateError(context)
                    return
                }
                is LocationResult.Unavailable -> {
                    Log.w(TAG, "EMERGENCY_HALTED: Location unavailable: ${locResult.reason}")
                    _stage.value = SosStage.Error(
                        reason = "Unable to acquire an accurate GPS location fix (${locResult.reason}). Please ensure you have satellite reception and retry.",
                        canRetry = true
                    )
                    HapticUtils.vibrateError(context)
                    return
                }
            }

            // 4. Format emergency message with user name and location
            _stage.value = SosStage.TransmittingSms
            val userName = userRepository.userProfile.value?.let {
                if (it.fullName.isNotBlank()) it.fullName else it.name
            } ?: authRepository.currentUser?.displayName ?: "SafeAlert User"

            val chosenMessage = when {
                customMessage.isNotBlank() -> customMessage
                else -> "I need immediate assistance."
            }

            val timestampStr = DateUtils.formatFullDateTime(System.currentTimeMillis())
            val messageText = if (triggerSource == "SHAKE" || triggerSource == "VOICE" || triggerSource == "AUTOMATIC") {
                // PART H exact automatic template
                buildString {
                    append("🚨 EMERGENCY ALERT\n\n")
                    append("I need immediate assistance.\n\n")
                    append("Name: $userName\n\n")
                    if (!mapsUrl.isNullOrBlank()) {
                        append("📍 My current location:\n$mapsUrl\n\n")
                    }
                    append("Time:\n$timestampStr\n\n")
                    append("Please contact me immediately.")
                }
            } else {
                buildString {
                    append("🚨 EMERGENCY ALERT\n\n")
                    append("Type: $emergencyType\n")
                    append("Name: $userName\n\n")
                    append(chosenMessage)
                    if (!mapsUrl.isNullOrBlank()) {
                        append("\n\n📍 Location:\n$mapsUrl")
                    }
                    append("\n\nTime: $timestampStr")
                    append("\n\nPlease contact me immediately.")
                }
            }

            // 4. Send SMS to all enabled trusted contacts
            val smsResult = smsSender.sendEmergencySms(contactsList, messageText)
            Log.i(TAG, "SMS_DISPATCH: successful=${smsResult.successfulCount}/${contactsList.size}")

            val fallbackIntent = if (smsResult.successfulCount == 0) {
                val phoneNumbers = contactsList.map { it.phoneNumber }
                smsSender.createFallbackSmsIntent(phoneNumbers, messageText)
            } else null

            // 5. Store alert audit in Firestore users/{uid}/alerts/{alertId}
            _stage.value = SosStage.SavingCloudAudit
            val alertRecord = EmergencyAlert(
                alertId = "",
                uid = userId,
                emergencyType = emergencyType,
                triggerSource = triggerSource,
                message = messageText,
                latitude = latitude,
                longitude = longitude,
                mapsUrl = mapsUrl,
                locationAvailable = (latitude != null && longitude != null),
                timestamp = System.currentTimeMillis(),
                recipients = contactsList.map { "${it.name} (${it.phoneNumber})" },
                recipientPhoneNumbers = contactsList.map { it.phoneNumber },
                recipientCount = contactsList.size,
                status = if (smsResult.successfulCount > 0) "SMS_SENT" else if (fallbackIntent != null) "SMS_COMPOSER_OPENED" else "SMS_FAILED",
                sendMethod = if (smsResult.successfulCount > 0) "Direct SMS" else "SMS Composer",
                failureReason = smsResult.failureReason,
                createdAt = System.currentTimeMillis()
            )

            try {
                alertsRepository.saveEmergencyAlert(userId, alertRecord)
                Log.d(TAG, "ALERT_SAVED to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "FIRESTORE_AUDIT_WARNING (offline safety active): ${e.message}")
            }

            if (smsResult.successfulCount > 0) {
                HapticUtils.vibrateEmergencyAlertSent(context)
                postNotification(
                    title = "🚨 Emergency Alert Dispatched ($triggerSource)",
                    message = "SMS dispatched to ${smsResult.successfulCount} trusted contact(s) with your GPS coordinates."
                )
                _stage.value = SosStage.Completed(alertRecord, smsResult)
            } else {
                HapticUtils.vibrateError(context)
                postNotification(
                    title = "❌ Emergency SMS Failed ($triggerSource)",
                    message = smsResult.failureReason ?: "Failed to transmit emergency SMS."
                )
                _stage.value = SosStage.Error(
                    reason = smsResult.failureReason ?: "Failed to transmit emergency SMS.",
                    canRetry = true,
                    fallbackIntent = fallbackIntent
                )
            }
        } finally {
            isDispatching = false
        }
    }

    private fun postNotification(title: String, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                ?: return

            val channelId = "emergency_alerts_channel"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Emergency Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High priority emergency alert status and dispatch notifications"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, com.siddesh.smsalerts.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                openIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.siddesh.smsalerts.R.drawable.ic_app_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post emergency notification: ${e.message}")
        }
    }
}
