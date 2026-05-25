# 🔮 CoreShell v2.1 — Stability & Security Release

<div align="center">

**Production-Ready SSH Client with Enterprise-Grade Encryption**

[![Release](https://img.shields.io/badge/Release-v2.1-00FF41?style=for-the-badge&logo=github)](https://github.com/PIXELQUADRO07/CoreShell/releases/tag/v2.1)
[![Security](https://img.shields.io/badge/Security-AES--256--GCM-FF006E?style=for-the-badge&logo=lock)](https://developer.android.com/training/articles/keystore)
[![SSH](https://img.shields.io/badge/SSH-Live%20Connections-00D9FF?style=for-the-badge&logo=openssh)](https://jcraft.com/jsch/)
[![Status](https://img.shields.io/badge/Status-Stable-00FF41?style=for-the-badge)](https://github.com/PIXELQUADRO07/CoreShell)

*From Simulation to Production: The Netrunner's Arsenal Reaches Enterprise Grade*

</div>

---

## 🎯 What's New in v2.1

### 🔴 **Critical Upgrades**

#### ✅ Real SSH Connection
```
BEFORE: ❌ Simulated connections with hardcoded delays
AFTER:  ✅ Live SSH sessions via JSch with real I/O
```
- `connectToServer()` establishes genuine SSH channels  
- Streams live server output directly to terminal  
- Commands execute on actual remote shell  
- Real stdin/stdout/stderr bidirectional communication  

#### 🔐 **End-to-End Encryption (AES-256-GCM)**
```
BEFORE: ❌ Plaintext passwords & keys in database
AFTER:  ✅ Military-grade encryption at rest
```
- All credentials encrypted with Android Keystore-backed AES-256-GCM  
- IV prepended and stored with ciphertext in Base64  
- Decryption happens only at connection time  
- Zero cleartext exposure in database or logs  

#### 🎭 **Terminal State Immutability Fixed**
```
BEFORE: ❌ Mutable state → missed UI updates
AFTER:  ✅ Immutable values → reliable recomposition
```
- `SshSessionState` transitioned to `val` fields  
- Copy-on-write updates via `copy()` lambda  
- Compose recomposition triggers on every change  
- Terminal UI now responds smoothly to all updates  

---

## 🟡 **Major Improvements**

| Feature | Improvement | Impact |
|---------|-------------|--------|
| **📊 Live Telemetry** | Real `top` & `free` commands (10s refresh) | CPU/RAM gauges show actual server metrics |
| **🏠 Widget Data** | Queries actual database instead of static list | Home screen shows your real servers |
| **🔑 SSH Crypto** | JSch 0.2.20 with modern algorithms | ssh-ed25519, ecdsa-sha2-nistp256, curve25519-sha256 support |
| **🪟 Windows Fix** | Disabled GSSAPI fallback | No more infinite hangs on Windows Kerberos |
| **🔓 Key Storage** | RSA-2048 & Ed25519 encryption before DB | Private keys secured with AES-256-GCM |
| **📐 Key Extraction** | Proper ASN.1 parsing | Robust Ed25519 public key handling |
| **📱 App Identity** | `com.tuonome.coreshell` | Professional package namespace |
| **🗄️ Database API** | Sync query method added | Widget provider support |
| **✅ Test Coverage** | SecurityUtils unit tests | Encryption/decryption validation |

---

## 🏗️ Technical Architecture

### Cryptography Pipeline
```
┌──────────────────────────────────────┐
│  User Input (Password / Private Key) │
└────────────────────┬─────────────────┘
                     │
                     ▼
        ┌────────────────────────┐
        │  SecurityUtils.encrypt() │
        │  • Generate random IV   │
        │  • AES-256-GCM cipher   │
        │  • Android Keystore     │
        └────────────┬────────────┘
                     │
                     ▼
        ┌────────────────────────┐
        │  [IV + Ciphertext]     │
        │  Base64 encode         │
        └────────────┬────────────┘
                     │
                     ▼
        ┌────────────────────────┐
        │  Room Database         │
        │  Encrypted Storage     │
        └────────────────────────┘
```

### SSH Session Lifecycle
```
User Input → connectToServer()
    │
    ├─→ Initialize JSch Session
    ├─→ Configure Algorithms (modern kex/cipher)
    ├─→ Disable GSSAPI (Windows compatibility)
    ├─→ Password/Key Authentication
    ├─→ Open Shell Channel
    ├─→ Stream I/O (stdin/stdout/stderr)
    └─→ Update SessionState via StateFlow
```

---

## 📊 Performance & Reliability

### Telemetry Updates
- **Frequency:** Every 10 seconds  
- **Commands:** `top -bn1`, `free -m`  
- **Parsing:** Regex extraction with fallback  
- **Resilience:** Graceful degradation on incompatible systems  

### Database Queries
```kotlin
// Widget provider - synchronous query
getAllProfilesSync(): List<ServerProfile>

// UI layer - Flow-based reactive
getAllProfiles(): Flow<List<ServerProfile>>
```

---

## ⚠️ Migration Guide

### Before Updating

1. **Export your server profiles** (recommended)  
2. Note any saved SSH keys for re-entry  
3. Backup your CoreShell database:  
   ```bash
   adb backup -f coreshell_backup.ab com.tuonome.coreshell
   ```

### After Updating

| Item | Action | Notes |
|------|--------|-------|
| **Saved Servers** | ⚠️ Re-enter all credentials | Plaintext → AES-256-GCM format change |
| **SSH Keys** | ✅ Preserved (encrypted) | Existing keys migrated automatically |
| **Database Schema** | ✅ No changes | Backward compatible |
| **Settings** | ✅ Preserved | Preferences remain intact |

---

## 🔒 Security Enhancements Summary

```
v2.0                          v2.1
─────────────────────────────────────────
Simulated SSH      →    Real Live Sessions
Plaintext Storage  →    AES-256-GCM Encrypted
Random Telemetry   →    Live Server Metrics
Basic Auth         →    Modern Cryptography
                        (ed25519, ecdsa, curve25519)
GSSAPI Hangs       →    Properly Disabled
Fragile Key Parsing →   ASN.1 Standards
```

---

## 📥 Installation & Compatibility

### Minimum Requirements
- **Android:** 7.0+ (API 24)  
- **Storage:** 50 MB free space  
- **Network:** Active connection for SSH  

### Tested Environments
✅ OpenSSH 7.x–9.x (Linux)  
✅ OpenSSH for Windows (via Tailscale)  
✅ MagicDNS (Tailscale)  
✅ Modern key algorithms (ed25519, ecdsa-sha2-nistp256)  
✅ Backward compatibility with older servers  
✅ Windows environments without Kerberos  

### Quick Install
```bash
# Download app-release.apk and install
adb install app-release.apk

# Or build from source
git clone https://github.com/PIXELQUADRO07/CoreShell.git
cd CoreShell
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 🧪 Testing & Validation

✅ **Unit Tests Added:**
- AES-256-GCM encryption/decryption round-trips  
- Empty string edge cases  
- IV prepending and extraction  
- Base64 encoding/decoding  

✅ **Integration Testing:**
- Real SSH connections to multiple server types  
- Credential encryption/decryption cycles  
- Terminal state mutations and UI updates  
- Widget provider database queries  

---

## 🚀 What's Next (v2.2+)

- [ ] Multi-session concurrent connections  
- [ ] SSH port forwarding & tunneling  
- [ ] Custom terminal color themes  
- [ ] Hardware security key support  
- [ ] Cloud-based profile sync  
- [ ] SSH certificate authentication  
- [ ] Performance optimizations  

---

## 📝 Known Limitations & Workarounds

| Issue | Workaround | Status |
|-------|-----------|--------|
| Windows servers without `top`/`free` | Telemetry falls back gracefully | ✅ Handled |
| First-time server setup required | Re-enter credentials once | ⚠️ Expected |
| Keystore unavailable after restore | Re-add affected servers | ⚠️ One-time |

---

## 💬 Feedback & Support

**Found an issue?** [Open an Issue](https://github.com/PIXELQUADRO07/CoreShell/issues)  
**Have suggestions?** [Start a Discussion](https://github.com/PIXELQUADRO07/CoreShell/discussions)  
**Security concerns?** Please report privately to the maintainer  

---

## 📋 Detailed Changelog

### 🔴 Critical Fixes
- Real SSH connection via JSch (no more simulation)  
- Passwords & private keys encrypted at rest (AES-256-GCM)  
- Terminal state immutability fixed (reactive UI updates)  

### 🟡 Improvements
- Real server telemetry (CPU/RAM gauges)  
- Widget reads actual server profiles  
- Updated JSch to 0.2.20 (modern algorithms)  
- Disabled GSSAPI authentication (Windows fix)  
- SSH key encryption before storage  
- Improved Ed25519 public key extraction (ASN.1)  
- Updated applicationId  
- Added synchronous DAO methods  
- Added SecurityUtils unit tests  

### 📦 Dependencies Updated
```
com.github.mwiede:jsch → 0.2.20 (from unmaintained original)
```

---

<div align="center">

### ⚡ CoreShell v2.1: Enterprise Security. Netrunner Aesthetics. Raw Performance.

**[📥 Download Latest](https://github.com/PIXELQUADRO07/CoreShell/releases/tag/v2.1)** • **[📖 Full Documentation](https://github.com/PIXELQUADRO07/CoreShell)** • **[🐛 Report Issues](https://github.com/PIXELQUADRO07/CoreShell/issues)**

*Built with ❤️ by [@PIXELQUADRO07](https://github.com/PIXELQUADRO07)*

**"In the darkness of the network, CoreShell is your light."**

</div>
