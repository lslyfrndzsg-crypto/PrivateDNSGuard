# PrivateDNS Guard 🛡️

A modern, privacy-focused Android application designed to secure, monitor, and lock Android's system-level **Private DNS (DNS-over-TLS)** configuration against tampering, accidental changes, or circumvention.

Built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Shizuku**.

---

## ✨ Features

- 🔒 **Tamper Protection & Auto-Revert**: A real-time `ContentObserver` monitors Android's `Settings.Global` (`private_dns_mode` & `private_dns_specifier`). If anyone or any app changes or turns off your secure DNS, PrivateDNS Guard immediately reverts the change in milliseconds.
- 🎯 **One-Tap "Lock Current System DNS"**: Manually configure your personal **NextDNS** (or any other DoT provider) directly in Android's standard Settings app, then open PrivateDNS Guard and tap **"Lock Current Setting as Guard Target"**!
- ⚡ **1-Tap Shizuku Integration**: Grants the required `WRITE_SECURE_SETTINGS` permission directly on your phone with a single tap—no PC or root needed.
- 💻 **Copyable ADB Command**: Step-by-step guidance and a 1-tap copy button for granting `WRITE_SECURE_SETTINGS` via a computer terminal if Shizuku is not used.
- ⏸️ **Snooze / Pause Timer**: Temporarily pause protection for 10 min, 30 min, 1 hr, or 2 hrs to log into hotel/airport captive portals where port 853 is blocked. Automatically re-locks when the timer expires!
- 🔐 **App Lock with PIN & Biometrics**: Protects opening the app and disarming the guard with Master PIN and Fingerprint / Face Unlock.
- 🛡️ **Anti-Uninstall (Optional Device Admin)**: Optional toggle to prevent unauthorized parties or children from uninstalling the app to bypass the DNS lock.
- 🚀 **Auto-Start on Boot**: Fully supports `BOOT_COMPLETED` and Direct Boot (`LOCKED_BOOT_COMPLETED`) to immediately resume protection after phone reboots.
- 📜 **Tamper Audit Logs**: Persistent Room database tracking all attempted modifications with timestamps, attempted values, and revert status.
- 🤖 **GitHub Actions CI/CD**: Automatically compiles and tests the APK on every push, providing direct download links in the GitHub Actions tab.

---

## 🛠️ How It Works with NextDNS

1. Open Android **Settings > Network & internet > Private DNS**.
2. Select **Private DNS provider hostname** and enter your personal NextDNS endpoint (e.g. `xyz123.dns.nextdns.io`).
3. Tap **Save**.
4. Open **PrivateDNS Guard**.
5. Tap **"Lock Current Setting as Guard Target"** (or enable the master Guard switch).
6. That's it! Your NextDNS endpoint is now actively guarded and locked.

---

## 🔑 Permissions & Setup

Modifying Android's Private DNS setting requires the system-level `WRITE_SECURE_SETTINGS` permission. PrivateDNS Guard supports two easy ways to grant it:

### Method 1: Shizuku (Recommended, No PC)
1. Install and start [Shizuku](https://shizuku.rikka.app/) on your phone (starts in ~30 seconds using Android's built-in Wireless Debugging).
2. Open PrivateDNS Guard -> Go to **Permission Setup**.
3. Tap **"Grant Permission via Shizuku"** and tap **Allow** on the prompt.

### Method 2: ADB (Via Computer)
1. Enable **Developer Options** & **USB Debugging** on your phone.
2. Connect your phone to your computer with a USB cable.
3. Open a terminal / command prompt and run:
   ```bash
   adb shell pm grant com.privatednsguard android.permission.WRITE_SECURE_SETTINGS
   ```

---

## 🚀 Building the APK via GitHub Actions

You don't need Android Studio or build tools installed on your computer.

1. Push this project to your GitHub repository:
   ```bash
   git init
   git add .
   git commit -m "Initial commit of PrivateDNS Guard"
   git remote add origin https://github.com/<your-username>/PrivateDNSGuard.git
   git push -u origin main
   ```
2. GitHub Actions will automatically trigger `.github/workflows/build.yml`.
3. Go to the **Actions** tab on your GitHub repository.
4. Select the latest workflow run and download **`PrivateDNSGuard-Debug-APK`**.
5. Open the downloaded APK on your Android phone to install!

---

## 📱 Local Development

- **Android Studio**: Ladybug / Koala or newer
- **Gradle**: 8.10.2
- **JDK**: Java 17
- **Min SDK**: Android 9.0 (API 28)
- **Target SDK**: Android 15 (API 35)

To build debug APK locally:
```bash
./gradlew assembleDebug
```
Output location:
`app/build/outputs/apk/debug/app-debug.apk`
