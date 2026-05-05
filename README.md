# CoreShell

CoreShell is a lightweight SSH manager for Android, designed to provide secure remote server access, file management via SFTP, and interactive shell sessions.

It is built with Kotlin and uses JSch for SSH communication.

---

## ✨ Features

- 🔐 SSH connection (password & private key authentication)
- 📡 Remote command execution
- 🖥️ Interactive shell sessions
- 📁 SFTP file manager (upload, download, rename, delete)
- 📊 Basic server info monitoring
- ⚡ Coroutine-based async operations (non-blocking UI)

---

## 🧱 Tech Stack

- Kotlin
- Android SDK
- JSch (SSH/SFTP)
- Kotlin Coroutines
- Gradle Kotlin DSL

---

## 📦 Project Structure


CoreShell/
├── app/ # Android application module
├── gradle/ # Gradle wrapper and version catalog
├── build.gradle.kts # Root build configuration
├── settings.gradle.kts # Project modules


---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK installed

### Build the project

```bash
./gradlew assembleDebug
Run on device/emulator

Open project in Android Studio and click Run ▶

🔐 Security Notes
Never commit local.properties
Never commit .jks or keystore files
Avoid storing SSH passwords in plaintext
Prefer SSH key authentication
📁 Build Outputs

APK files are generated in:

app/build/outputs/apk/
⚠️ Disclaimer

This project is intended for educational and personal use.
Improper use of SSH access may lead to security risks.

📌 Future Improvements
Tab-based multi-session manager
Encrypted credential storage (Android Keystore)
Host key verification support
UI improvements (Material 3)
Session persistence
