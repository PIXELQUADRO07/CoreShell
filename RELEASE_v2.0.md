# CoreShell - Secure Cybernetic Terminal Deck

CoreShell is an SSH client and server node manager for Android, designed with a retro-futuristic Cyberpunk aesthetic. It offers an advanced interface for managing remote infrastructure, with integrated Tailscale support and a real-time telemetry dashboard.

![App Header](app/src/main/res/drawable/img_start_banner_1779391997906.png)

## 🚀 Key Features

- **Server Profile Manager**: Configure and save your SSH nodes with custom parameters (Host, Port, Username).
- **Tailscale Integration**: Automatic detection and optimized support for Tailscale networks (MagicDNS and 100.x.x.x IPs).
- **Cyberpunk Aesthetic**: Reactive UI interface with CRT scanline effects, neon animations, and monospace fonts.
- **Advanced Terminal**: Interactive shell with quick shortcuts for common commands (`ls`, `htop`, `neofetch`, etc.).
- **SFTP File Explorer**: Browse, download, edit, and delete files on remote servers through an integrated graphical interface.
- **Key Manager (RSA Keyring)**: Generate and manage RSA-2048 key pairs for secure password-less authentication.
- **Telemetry Monitoring**: Real-time visualization of CPU, RAM, temperature, and network traffic through oscilloscope-style graphs.
- **Monitor Widget**: Home screen widget that displays your favorite server status at a glance.

## 🛠️ Technologies Used

- **Kotlin & Jetpack Compose**: Modern and declarative UI.
- **Room Database**: Secure local persistence for profiles and keys.
- **Coroutines & Flow**: Asynchronous session and telemetry management.
- **Material 3**: UI components with "Cyber-Theme" customizations.
- **Retrofit & Moshi**: Ready for external API integrations.

## 📱 Installation and Build

### Requirements
- Android 7.0 (API 24) or higher.
- Android Studio Ladybug (or recent versions).
- Gradle 9.3.1.

### Build from Terminal
To generate the release APK:
```bash
./gradlew assembleRelease
```
The APK will be generated at: `app/build/outputs/apk/release/app-release.apk`

## 🔒 Security
CoreShell supports password and RSA key authentication. Private keys are stored locally in the app's encrypted database. Tailscale is recommended for an additional layer of network security.

## 📝 Current Version Notes
*   The current version uses a simulation engine for SSH/SFTP sessions and telemetry.
*   Integration with real SSH libraries (such as JSch) is planned for upcoming iterations.

---
*Developed for the underground netrunner community.*
