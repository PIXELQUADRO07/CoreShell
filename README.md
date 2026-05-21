

<img width="383" height="850" alt="CoreShell3" src="https://github.com/user-attachments/assets/1edf455f-4938-4541-84c2-2292b98a6dd9" />
<img width="372" height="851" alt="CoreShell2" src="https://github.com/user-attachments/assets/e8c69fb0-fe6d-4cfe-a89c-df499cfe0e50" />
<img width="377" height="851" alt="CoreShell" src="https://github.com/user-attachments/assets/a0facd38-4f56-41be-9277-3097ae319f8b" />
# 🔮 CoreShell - Secure Cybernetic Terminal Deck

<div align="center">

**A Cyberpunk-Inspired SSH Manager for Android**

[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-7.0+-3DDC84?style=for-the-badge&logo=android)](https://www.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Modern%20UI-4285F4?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

*Your gateway to the netrunner's underground*

</div>

---

## 📸 Visual Overview

![App Header](app/src/main/res/drawable/img_start_banner_1779391997906.png)

---

## ✨ Core Features

### 🖥️ **Server Profile Manager**
Configure and save your SSH nodes with custom parameters:
- Host, Port, and Username configuration
- Connection history and quick access
- Profile categorization and tagging

### 🌐 **Tailscale Integration**
Seamless support for Tailscale networks:
- Automatic network detection
- MagicDNS and 100.x.x.x IP support
- One-click VPN integration

### 🎨 **Cyberpunk Aesthetics**
Immersive retro-futuristic interface:
- CRT scanline effects and glow animations
- Neon-themed UI components
- Monospace typography for authentic terminal feel
- Animated transitions and visual feedback

### 💻 **Advanced Terminal Shell**
Interactive command execution with enhanced UX:
- Quick-access shortcuts for common commands (`ls`, `htop`, `neofetch`, etc.)
- Real-time command history
- Copy/paste functionality with Android clipboard integration

### 📂 **SFTP File Explorer**
Full-featured file management on remote servers:
- Browse remote directories with hierarchical view
- Download, upload, modify, and delete files
- File preview and metadata display
- Drag-and-drop support (where applicable)

### 🔐 **RSA Key Manager (Keyring)**
Enterprise-grade key management:
- Generate RSA-2048 key pairs
- Secure local storage in encrypted database
- Passwordless SSH authentication
- Key rotation and management utilities

### 📊 **Real-time Telemetry Monitoring**
Live system metrics visualization:
- CPU usage with oscilloscope-style graphs
- RAM consumption tracking
- System temperature monitoring
- Network traffic analysis with bandwidth gauges

### 📱 **Home Screen Widget**
Quick overview of server status:
- At-a-glance server health metrics
- One-tap connection launch
- Customizable widget layout

---

## 🛠️ Technology Stack

```
┌─────────────────────────────────────┐
│   Frontend & UI                     │
├─────────────────────────────────────┤
│ • Kotlin & Jetpack Compose          │
│ • Material 3 Design System          │
│ • Custom Cyberpunk Theme            │
│ • Canvas-based Graphics             │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   Data & Persistence                │
├─────────────────────────────────────┤
│ • Room Database (encrypted)         │
│ • DataStore for preferences         │
│ • Secure SharedPreferences          │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   Async & Reactive                  │
├─────────────────────────────────────┤
│ • Kotlin Coroutines                 │
│ • Flow & StateFlow                  │
│ • ViewModel & MVVM architecture     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   Networking & Integration          │
├─────────────────────────────────────┤
│ • Retrofit 2 for HTTP               │
│ • Moshi for JSON serialization      │
│ • OkHttp with interceptors          │
└─────────────────────────────────────┘
```

---

## 📱 Installation & Build

### ⚙️ Requirements

| Component | Version |
|-----------|---------|
| **Android** | 7.0+ (API 24) |
| **Android Studio** | Ladybug or recent |
| **Gradle** | 9.3.1+ |
| **Java/Kotlin** | JDK 17+ |

### 🚀 Quick Build

**Generate Release APK:**
```bash
./gradlew assembleRelease
```

**Output Location:**
```
📦 app/build/outputs/apk/release/app-release.apk
```

**Install to Device:**
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

**Debug Build (Development):**
```bash
./gradlew installDebug
```

---

## 🔒 Security & Privacy

CoreShell implements multiple security layers:

| Feature | Description |
|---------|-------------|
| **Authentication** | Password & RSA key-based SSH authentication |
| **Encryption** | Local private keys stored in encrypted Room database |
| **Network** | Tailscale VPN integration for secure transport |
| **Permissions** | Minimal Android permissions requested |
| **Best Practices** | No cleartext storage, secure communication protocols |

### 🛡️ Security Recommendations

- Use RSA key authentication instead of passwords when possible
- Enable Tailscale for encrypted network tunneling
- Regularly rotate SSH keys
- Keep your Android device updated with latest security patches

---

## 📊 Architecture Overview

```
┌──────────────────────────────────────────────┐
│         UI Layer (Jetpack Compose)           │
├──────────────────────────────────────────────┤
│  Screens │ Components │ Themes │ Animations  │
└────────────────────┬─────────────────────────┘
                     │
┌────────────────────▼─────────────────────────┐
│        ViewModel & State Management          │
├──────────────────────────────────────────────┤
│    MVVM │ StateFlow │ Coroutines             │
└────────────────────┬─────────────────────────┘
                     │
┌────────────────────▼─────────────────────────┐
│     Repository & Data Access Layer           │
├──────────────────────────────────────────────┤
│    Room │ DataStore │ Remote APIs            │
└────────────────────┬─────────────────────────┘
                     │
┌────────────────────▼─────────────────────────┐
│      SSH/SFTP & System Services              │
├──────────────────────────────────────────────┤
│    Sessions │ File Transfer │ Telemetry      │
└──────────────────────────────────────────────┘
```

---

## 📝 Current Version Notes

⚠️ **Important Information:**

- The current version includes **simulation engine** for SSH/SFTP sessions and telemetry
- Integration with production SSH libraries (like **JSch**) is planned for upcoming releases
- UI/UX is production-ready; backend integration is in progress
- Feature parity with desktop SSH clients is the goal

---

## 🚀 Roadmap

- [ ] Full JSch SSH/SFTP implementation
- [ ] Multi-session support
- [ ] SSH port forwarding & tunneling
- [ ] Custom terminal themes
- [ ] Cloud backup for profiles
- [ ] Hardware key support
- [ ] Dark mode enhancements

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**PIXELQUADRO07**
- GitHub: [@PIXELQUADRO07](https://github.com/PIXELQUADRO07)

---

<div align="center">

### ⚡ Built with ❤️ for the underground netrunner community

*"In the darkness of the network, CoreShell is your light."*

[![Follow](https://img.shields.io/badge/Follow-000000?style=for-the-badge&logo=github)](https://github.com/PIXELQUADRO07)

</div>
