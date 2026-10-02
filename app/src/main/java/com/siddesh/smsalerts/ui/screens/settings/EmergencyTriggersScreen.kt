package com.siddesh.smsalerts.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.siddesh.smsalerts.data.preferences.UserPreferencesDataStore
import com.siddesh.smsalerts.data.sensor.ShakeDetector
import com.siddesh.smsalerts.data.voice.VoiceEmergencyDetector
import com.siddesh.smsalerts.ui.components.GlassCard
import com.siddesh.smsalerts.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyTriggersScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesDataStore = remember { UserPreferencesDataStore(context) }
    val preferences by preferencesDataStore.preferencesFlow.collectAsState(
        initial = com.siddesh.smsalerts.data.preferences.AppUserPreferences()
    )

    // Hardware checks
    val isShakeSupported = remember { ShakeDetector(context) {}.isSupported() }
    val isVoiceSupported = remember { VoiceEmergencyDetector(context, {}).isSupported() }

    // Permission launcher for RECORD_AUDIO
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch { preferencesDataStore.updateVoiceAlert(true) }
            Toast.makeText(context, "Voice trigger enabled. Say HELP to test.", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch { preferencesDataStore.updateVoiceAlert(false) }
            Toast.makeText(context, "Microphone permission is required for voice commands.", Toast.LENGTH_LONG).show()
        }
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        com.siddesh.smsalerts.service.EmergencyProtectionService.startService(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Emergency Triggers",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // INFO BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmergencyRed.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, EmergencyRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EmergencyRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Unified Emergency Engine",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All triggers dispatch through the same emergency pipeline, ensuring instant location attachment and recipient broadcast.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // 1. EMERGENCY BUTTON
            TriggerSettingCard(
                title = "Emergency Button",
                description = "Enable visible Emergency Alert button on the Home dashboard.",
                icon = Icons.Default.TouchApp,
                iconTint = EmergencyRed,
                checked = preferences.emergencyButtonEnabled,
                onCheckedChange = { checked ->
                    scope.launch { preferencesDataStore.updateEmergencyButtonEnabled(checked) }
                }
            )

            // 2. SHAKE TO ALERT
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Shake to Alert",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isShakeSupported) {
                                        "Trigger an emergency alert after detecting deliberate shakes."
                                    } else {
                                        "Shake detection isn't supported on this device."
                                    },
                                    color = if (isShakeSupported) TextSecondary else StatusWarning,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = preferences.shakeToAlertEnabled && isShakeSupported,
                            onCheckedChange = { checked ->
                                if (isShakeSupported) {
                                    scope.launch {
                                        preferencesDataStore.updateShakeToAlert(checked)
                                        if (checked) {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            com.siddesh.smsalerts.service.EmergencyProtectionService.startService(context)
                                        } else {
                                            com.siddesh.smsalerts.service.EmergencyProtectionService.stopService(context)
                                        }
                                    }
                                }
                            },
                            enabled = isShakeSupported,
                            colors = switchColors()
                        )
                    }

                    if (preferences.shakeToAlertEnabled && isShakeSupported) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Required Shakes Selection
                        Text(
                            text = "REQUIRED SHAKES: ${preferences.requiredShakes}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 4, 5).forEach { count ->
                                FilterChip(
                                    selected = preferences.requiredShakes == count,
                                    onClick = {
                                        scope.launch { preferencesDataStore.updateRequiredShakes(count) }
                                    },
                                    label = { Text("$count shakes") },
                                    colors = chipColors()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Shake Cooldown
                        Text(
                            text = "TRIGGER COOLDOWN: ${preferences.shakeCooldownSeconds}s",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30 to "30 sec", 60 to "1 min", 300 to "5 min").forEach { (secs, label) ->
                                FilterChip(
                                    selected = preferences.shakeCooldownSeconds == secs,
                                    onClick = {
                                        scope.launch { preferencesDataStore.updateShakeCooldown(secs) }
                                    },
                                    label = { Text(label) },
                                    colors = chipColors()
                                )
                            }
                        }
                    }
                }
            }

            // 3. VOICE EMERGENCY TRIGGER
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Voice Emergency Trigger",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isVoiceSupported) {
                                        "Say HELP, HELP ME, or EMERGENCY to start emergency dispatch."
                                    } else {
                                        "Speech recognition is unavailable on this device."
                                    },
                                    color = if (isVoiceSupported) TextSecondary else StatusWarning,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = preferences.voiceAlertEnabled && isVoiceSupported,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        scope.launch { preferencesDataStore.updateVoiceAlert(true) }
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                } else {
                                    scope.launch { preferencesDataStore.updateVoiceAlert(false) }
                                }
                            },
                            enabled = isVoiceSupported,
                            colors = switchColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Privacy: Safe Alerts uses on-device Android speech recognition only when active. Microphone audio is never recorded to disk or uploaded.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // 4. EMERGENCY COUNTDOWN & CONFIRMATION
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Emergency Countdown",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${preferences.emergencyCountdownSeconds} seconds cancellation window before sending.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10).forEach { sec ->
                            FilterChip(
                                selected = preferences.emergencyCountdownSeconds == sec,
                                onClick = {
                                    scope.launch { preferencesDataStore.updateCountdownDuration(sec) }
                                },
                                label = { Text("$sec seconds") },
                                colors = chipColors()
                            )
                        }
                    }
                }
            }

            // 5. REQUIRE CONFIRMATION
            TriggerSettingCard(
                title = "Require Confirmation",
                description = "Shows confirmation & review screen prior to final carrier SMS transmission.",
                icon = Icons.Default.CheckCircle,
                iconTint = Color(0xFFA855F7),
                checked = preferences.requireConfirmation,
                onCheckedChange = { checked ->
                    scope.launch { preferencesDataStore.updateRequireConfirmation(checked) }
                }
            )

            // 6. LOCATION ATTACHMENT
            TriggerSettingCard(
                title = "Location Attachment",
                description = "Includes clickable Google Maps link in all emergency alerts.",
                icon = Icons.Default.LocationOn,
                iconTint = Color(0xFF38BDF8),
                checked = preferences.locationSharingEnabled,
                onCheckedChange = { checked ->
                    scope.launch { preferencesDataStore.updateLocationSharing(checked) }
                }
            )

            // 7. ALLOW SMS WITHOUT LOCATION
            TriggerSettingCard(
                title = "Allow SMS Without Location",
                description = "If GPS is unavailable or permission denied, sends emergency SMS without coordinates instead of failing.",
                icon = Icons.Default.CloudDone,
                iconTint = StatusSuccess,
                checked = preferences.allowSmsWithoutLocation,
                onCheckedChange = { checked ->
                    scope.launch { preferencesDataStore.updateAllowSmsWithoutLocation(checked) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TriggerSettingCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = switchColors()
            )
        }
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = EmergencyRed,
    checkedTrackColor = EmergencyRed.copy(alpha = 0.3f),
    uncheckedThumbColor = TextMuted,
    uncheckedTrackColor = SurfaceDark
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = EmergencyRed.copy(alpha = 0.2f),
    selectedLabelColor = EmergencyRed,
    containerColor = SurfaceCard,
    labelColor = TextSecondary
)
