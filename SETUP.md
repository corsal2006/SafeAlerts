# SafeAlert — Developer Setup & Installation Guide

This guide provides complete instructions to set up, build, and run the **SafeAlert SMS Emergency Alert System** using Android Studio or the command line.

---

## Prerequisites

1. **Operating System**: Windows, macOS, or Linux.
2. **Android Studio**: Android Studio Hedgehog / Jellyfish / Koala / Ladybug or newer.
3. **Java Development Kit**: JDK 17 (Microsoft OpenJDK 17 or Eclipse Temurin 17).
4. **Android SDK**:
   - `compileSdk`: 34 (Android 14)
   - `minSdk`: 24 (Android 7.0 Nougat)
   - `targetSdk`: 34
5. **Physical Device**:
   - Android smartphone running Android 7.0+
   - Active SIM card with cellular network plan for real SMS testing
   - USB cable with USB Debugging enabled

---

## Option 1: Setup & Run with Android Studio (Recommended)

1. **Launch Android Studio**.
2. Click **Open** (or **File** → **Open...**) and select the project folder:
   ```text
   C:\Users\megha\sms
   ```
3. Android Studio will recognize the Gradle project and automatically trigger Gradle Sync.
4. **Configure JDK 17 in Android Studio**:
   - Navigate to **Settings** (or **Preferences** on macOS) → **Build, Execution, Deployment** → **Build Tools** → **Gradle**.
   - Under **Gradle JDK**, verify or set the JDK to **Java 17** (or the bundled Android Studio JBR).
5. **Add your Firebase configuration**:
   - Download your project's `google-services.json` from the [Firebase Console](https://console.firebase.google.com/).
   - Place it inside the `app/` directory (replacing the template `app/google-services.json`).
   - Detailed Firebase steps are available in [FIREBASE_SETUP.md](file:///c:/Users/megha/sms/FIREBASE_SETUP.md).
6. **Connect your Android phone**:
   - On your phone, go to **Settings** → **About Phone** → Tap **Build Number** 7 times to enable Developer Options.
   - Go to **Developer Options** → Enable **USB Debugging**.
   - Connect the phone to your computer via USB; allow USB debugging when prompted on the screen.
7. **Run the App**:
   - Select your physical device from the device drop-down in Android Studio's top toolbar.
   - Click the green **Run** button (or press `Shift + F10`).
   - The APK will compile, install, and launch automatically on your phone.

---

## Option 2: Command-Line Build & Installation

If you prefer building and installing via terminal (PowerShell or Bash):

### 1. Set Environment Variables
In PowerShell:
```powershell
$env:JAVA_HOME = "C:\Program Files (x86)\Android\openjdk\jdk-17.0.14"
$env:ANDROID_HOME = "C:\Users\megha\AppData\Local\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
```

### 2. Verify Android Device Connection
Connect your Android phone via USB and run:
```powershell
adb devices
```
You should see your device listed (e.g. `1234567890ABCDEF    device`).

### 3. Build the Debug APK
```powershell
.\gradlew.bat assembleDebug
```
The output APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install Directly to Connected Phone
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 5. Launch the Application
```powershell
adb shell am start -n com.safealert.app/.MainActivity
```

---

## Initial In-App Setup

Once launched on your phone:
1. **Grant Permissions**:
   - When prompted, grant **SEND_SMS** and **ACCESS_FINE_LOCATION**.
   - These permissions are required for the core WMC demonstration.
2. **Register an Account**:
   - Tap **Create Account**.
   - Enter your name, email, phone number, and a secure password.
3. **Add Emergency Contacts**:
   - Navigate to the **Contacts** tab.
   - Tap the **+** button and add at least one trusted phone number.
4. **Trigger First SOS Alert**:
   - On the **Home** tab, choose an emergency type (e.g., Medical Emergency).
   - Press and hold the circular **SOS** button for 2 seconds.
   - Let the 5-second countdown finish.
   - SafeAlert will acquire your real GPS location, transmit the emergency SMS, and log the event to Firebase!
