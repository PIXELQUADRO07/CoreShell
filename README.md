# CoreShell - Modern SSH & SFTP Client for Android

CoreShell is a powerful, secure, and user-friendly SSH and SFTP client designed for Android. It allows you to manage remote servers, execute commands in a terminal, and handle file transfers seamlessly.

## 🚀 Key Features

### 💻 Advanced SSH Terminal
- **Interactive Shell**: Full-featured terminal with support for interactive commands.
- **ANSI Color Support**: Vivid command output for better readability.
- **Persistent Sessions**: Maintain connections while switching between app features.
- **Quick Commands**: Save and execute your most-used commands with one tap.

### 📁 SFTP File Manager
- **File Operations**: Upload, download, rename, delete, and create directories on your remote server.
- **Visual Browser**: Intuitive UI for navigating remote file systems.
- **Transfer Progress**: Real-time tracking of uploads and downloads.

### 🗄️ Server Management (Profiles)
- **Profile Storage**: Save unlimited server profiles (Host, Port, Username, etc.).
- **Dashboard**: View server status and quick-connect from the main screen.
- **Last Connected**: Automatically tracks when you last accessed each server.

### 🛡️ Security & Privacy
- **Encryption**: Sensitive data like passwords and private keys are encrypted using **AES-256 (Android Keystore)**.
- **Host Key Verification**: Protection against Man-In-The-Middle (MITM) attacks with manual key confirmation.
- **Authentication**: Supports both Password and Public Key (Private Key) authentication.

### 📊 System Monitoring
- **Real-time Stats**: View remote CPU usage, RAM availability, and Uptime directly within the app.

---

## 🛠️ Technical Stack
- **UI Framework**: Native Android (XML) with Material Design 3.
- **Navigation**: Jetpack Navigation Component with Bottom Navigation.
- **Database**: Room Persistence Library for local server storage.
- **SSH/SFTP**: JSch (Modern Fork) for reliable network protocols.
- **Concurreny**: Kotlin Coroutines & Flow for asynchronous operations.
- **Architecture**: MVVM (Model-View-ViewModel) for clean code separation.

## 📦 Getting Started
1. Clone the repository.
2. Open in Android Studio.
3. Build the project using JDK 21.
4. Deploy to your Android device (Min SDK 26).

---
*Developed with focus on speed and security.*
