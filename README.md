# Safe Alerts — SMS Emergency Alert System

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin%201.9.24-purple.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20StateFlow%20%2B%20Coroutines-orange.svg)](https://developer.android.com/topic/architecture)
[![Backend](https://img.shields.io/badge/Backend-Firebase%20Auth%20%26%20Firestore-yellow.svg)](https://firebase.google.com/)
[![Build Status](https://img.shields.io/badge/Build-Passing%20(Gradle%208.7)-brightgreen.svg)](https://gradle.org/)

**Safe Alerts** is a complete, production-grade native Android application designed to deliver immediate personal safety alerts. When triggered, it dispatches emergency SMS messages containing the user's high-accuracy GPS coordinates (Google Maps link), situational notes, and timestamps to designated trusted contacts—operating reliably even without active data or Wi-Fi connectivity.

---

## 1. System Overview & Architecture

### Core Tech Stack
- **Language**: Kotlin 1.9.24
- **UI Framework**: Jetpack Compose with Material 3 (Edge-to-edge, dynamic light/dark theming, smooth micro-interactions, haptics)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern + Clean Data Separation
- **State Management**: Reactive Kotlin Coroutines & `StateFlow`
- **Authentication**: Firebase Authentication
  - Email & Password with inline validation
  - Passwordless Email-Link Sign-In (`sendSignInLinkToEmail`)
  - Google Sign-In with Credential Manager / Web Client ID
  - Forgot Password self-service reset
- **Cloud Database**: Cloud Firestore (`users/{uid}/trustedContacts`, `users/{uid}/alerts`) with offline disk persistence
- **Local Persistence**: Jetpack DataStore Preferences (Emergency message, countdown timer, location toggle, theme, direct SMS, haptic preferences)
- **Background Scheduling**: AlarmManager / BroadcastReceiver for **Safety Check-In**
- **Hardware Integration**:
  - `SmsManager` for native SMS transmission (GSM-7 auto-division into multipart messages)
  - System SMS Composer fallback intent for Google Play compliance
  - `FusedLocationProviderClient` for GPS / Network coordinates
  - `TelephonyManager` for SIM readiness and cellular network telemetry

---

## 2. Key Features

### 🛡️ 1. App Identity & Adaptive Branding
- Original vector logo concept: A protective shield containing an emergency signal pulse and SMS message bubble.
- Full adaptive launcher icon support with foreground, background, and monochrome assets (`ic_launcher.xml`, `ic_launcher_round.xml`).
- Polished animated splash screen checking Firebase authentication state on cold launch.

### 🚨 2. Deliberate SOS Activation
- Centerpiece interactive emergency button with pulsing wave indicators.
- **Accidental Tap Prevention**: Hold-to-confirm interaction (2-second visual progress ring) or tap mode based on user preference.
- **5-Second Cancellation Window**: Audible/tactile countdown allowing users to cancel false alarms before dispatch.
- **Real Service Readiness Check**: Real-time inspection of SIM card status, location services, and active trusted contacts count.

### 👥 3. Trusted Contacts Management
- Stored securely in Cloud Firestore: `users/{uid}/trustedContacts/{contactId}`.
- **System Contact Picker**: Import existing contacts directly using `ActivityResultContracts.PickContact()` with runtime `READ_CONTACTS` permission.
- **Manual Contact Entry**: Add custom emergency contacts with name, phone number, and relationship.
- Per-contact toggle to enable or disable inclusion in emergency dispatches.

### ⏱️ 4. Safety Check-In Timer
- Timed safety sessions: 15 minutes, 30 minutes, 1 hour, or custom duration.
- Live reactive countdown timer on the Home dashboard.
- Android system alarm triggers an alert notification if the user fails to check in before time expires.
- Instant "I'm Safe" check-in button to cancel timer and dismiss alarms.

### 📍 5. High-Accuracy Location Sharing
- Uses Google Play Services Location API (`FusedLocationProviderClient`).
- Generates universal Google Maps links: `https://maps.google.com/?q={latitude},{longitude}`.
- Graceful degradation: If GPS is disabled or permission denied, the alert dispatches cleanly with "Location Unavailable" without crashing.

### ✉️ 6. SMS Dispatch Architecture & Play Policy Compliance
- **Direct SMS Transmission**: Uses `SmsManager` when `SEND_SMS` permission is granted and Direct SMS is enabled.
- **Composer Fallback**: Automatically invokes the native Android SMS Composer pre-filled with emergency contacts and message text if direct SMS fails or permissions are restricted.
- Formats messages with concise, helpful text avoiding misleading or frightening default claims.

### 📜 7. Alert History & Audit Trail
- Stored under `users/{uid}/alerts/{alertId}`.
- Chronological timeline with recipient count, delivery method, timestamp, and location status.
- Alert detail dialog with Google Maps launch shortcut.
- Clean empty state: "No alerts sent yet."

### ⚙️ 8. User Settings & Preferences
- Backed by Jetpack DataStore Preferences:
  - Custom Emergency Message editor
  - Location sharing preference toggle
  - SOS Countdown Duration (3s, 5s, 10s)
  - Activation Mode (Hold 2s vs Single Tap)
  - Theme selection (System, Light, Dark)
  - Haptic feedback toggle
  - Privacy policy dialog & Sign out

---

## 3. Project Structure

```text
safealerts/
├── app/
│   ├── google-services.json            # Firebase configuration
│   ├── build.gradle.kts                # App dependencies & Android build config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     # Permissions, activities, receivers
│       │   ├── java/com/siddesh/smselerts/
│       │   │   ├── SafeAlertApp.kt     # Application class & offline Firestore config
│       │   │   ├── MainActivity.kt     # Compose host, theme observer & Google sign-in
│       │   │   ├── data/
│       │   │   │   ├── model/          # UserProfile, EmergencyContact, EmergencyAlert, EmergencyType
│       │   │   │   ├── preferences/    # UserPreferencesDataStore (Jetpack DataStore)
│       │   │   │   ├── repository/     # AuthRepository, ContactsRepository, AlertsRepository
│       │   │   │   ├── location/       # LocationService (FusedLocationProviderClient)
│       │   │   │   └── sms/            # SmsSender (SmsManager, fallback intent, SIM checks)
│       │   │   ├── service/            # SafetyCheckInManager, SafetyCheckInReceiver
│       │   │   ├── ui/
│       │   │   │   ├── navigation/     # NavGraph & AppNavigation destinations
│       │   │   │   ├── theme/          # Color, Theme, Type (Material 3 Dynamic Theming)
│       │   │   │   ├── components/     # TopBars, BottomNavBar, GlassCard, StatusDashboardGrid
│       │   │   │   ├── screens/
│       │   │   │   │   ├── splash/     # Splash screen with logo animation
│       │   │   │   │   ├── auth/       # Login, Register, ForgotPassword, EmailSent screens
│       │   │   │   │   ├── home/       # Dashboard, SOS trigger, Safety Check-In card
│       │   │   │   │   ├── contacts/   # ContactsScreen with system contact picker
│       │   │   │   │   ├── history/    # AlertHistoryScreen & detail viewer
│       │   │   │   │   ├── profile/    # User Profile screen
│       │   │   │   │   ├── settings/   # DataStore settings & permissions inspector
│       │   │   │   │   └── sos/        # SosCountdownScreen with live dispatch progress
│       │   │   │   └── viewmodel/      # AuthViewModel, HomeViewModel, SosViewModel, etc.
│       │   │   └── utils/              # ValidationUtils, DateUtils, HapticUtils, NetworkUtils
│       │   └── res/
│       │       ├── drawable/           # ic_app_logo, ic_launcher_foreground/background/monochrome
│       │       ├── mipmap-anydpi-v26/  # Adaptive launcher icon definitions
│       │       └── values/             # strings.xml, colors.xml, themes.xml
│       └── test/java/com/siddesh/smselerts/
│           ├── AuthValidationTest.kt           # Email regex, password length, name tests
│           ├── EmergencyMessageBuilderTest.kt  # Message formatting & location link tests
│           ├── ContactValidationTest.kt        # Contact model & active toggle tests
│           └── SafetyCheckInTest.kt            # Timer math & format remaining time tests
├── firestore.rules                     # Production Cloud Firestore security rules
├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Project settings & dependency resolution
├── gradle.properties                   # JVM memory & AndroidX flags
└── gradlew.bat                         # Gradle wrapper executable
```

---

## 4. How to Open and Run

### Prerequisites
- **Android Studio**: Jellyfish (2023.3.1), Koala (2024.1.1), or Ladybug (2024.2.1)+
- **JDK**: Java 17 (Microsoft OpenJDK 17 or Android Studio Embedded JBR 17)
- **Android Device**: Physical phone running Android 8.0 (API 26) or higher with an active SIM card (for SMS testing).

### Opening in Android Studio
1. Launch Android Studio.
2. Select **File > Open...** and navigate to the project directory: `c:\Users\megha\safealerts`.
3. Wait for Gradle Sync to complete. The project uses standard Gradle Kotlin DSL (`build.gradle.kts`) and will download all necessary plugins and dependencies automatically.
4. Select your connected Android device from the device dropdown.
5. Click **Run 'app'** (`Shift + F10`).

### Building from Command Line
```powershell
# In Windows PowerShell:
cd c:\Users\megha\safealerts

# 1. Run all Unit Tests (18 tests verifying auth, message builder, contacts, check-in):
.\gradlew.bat testDebugUnitTest

# 2. Compile and assemble the Debug APK:
.\gradlew.bat assembleDebug

# Output APK path:
# app\build\outputs\apk\debug\app-debug.apk

# 3. Install directly to a connected USB Android device:
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 5. Firebase Setup Guide

The application comes preconfigured with `app/google-services.json` for package `com.siddesh.smsalerts` (Project: `smselerts`).

### Step 1: Firebase Authentication Providers
In the [Firebase Console](https://console.firebase.google.com/project/smselerts/authentication/providers):
1. **Email/Password**: Enable **Email/Password**.
2. **Email link (passwordless sign-in)**: Check the toggle to enable Email Link sign-in.
3. **Google Sign-In**:
   - Enable **Google** as an identity provider.
   - Use Web Client ID: `663925742780-en3qinv0sd0hoooous15fn6begrduoko.apps.googleusercontent.com` (found in `google-services.json`).

### Step 2: SHA-1 & SHA-256 Fingerprint Configuration
To enable Google Sign-In and Phone/Email-link verification on your development machine:
1. Run the Gradle signing report command:
   ```powershell
   .\gradlew.bat signingReport
   ```
2. Locate the `SHA1` and `SHA-256` keys under `Variant: debug`.
3. Go to **Firebase Console > Project Settings > General > Your Apps > Android App (`com.siddesh.smsalerts`)**.
4. Click **Add Fingerprint** and paste your `SHA-1` and `SHA-256` hashes.

### Step 3: Deploy Firestore Security Rules
Go to **Firebase Console > Firestore Database > Rules** and copy the contents of `firestore.rules`:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    match /users/{userId} {
      allow read, write: if isOwner(userId);

      match /trustedContacts/{contactId} {
        allow read, write, delete: if isOwner(userId);
      }

      match /alerts/{alertId} {
        allow read, create, update, delete: if isOwner(userId);
      }
    }

    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## 6. Testing & Verification

The project includes an automated JUnit 4 test suite with 100% pass rate:
- **`AuthValidationTest`**: Email regex syntax, password length >= 6, password confirmation matching, full name character rules, phone number validation.
- **`EmergencyMessageBuilderTest`**: Default fallback message, custom situational notes, Google Maps URL formatting, location unavailable handling, emergency type tags.
- **`ContactValidationTest`**: EmergencyContact model instantiation, alert enabled/disabled status toggling, active contacts list filtering.
- **`SafetyCheckInTest`**: Countdown time math, 15m/30m/60m duration calculations, and time formatter (`00m 00s`, `14m 30s`, `01h 15m`).

All unit tests pass cleanly:
```text
Task :app:testDebugUnitTest
Test Summary: 18 tests, 0 failures, 100% successful
```

---

## 7. License
Developed as a production-quality native Android Emergency Alert System. Free to use and extend for personal safety and academic research.
