# CoreShell - Secure Cybernetic Terminal Deck

CoreShell è un client SSH e gestore di nodi server per Android, progettato con un'estetica Cyberpunk retrò-futuristica. Offre un'interfaccia avanzata per la gestione di infrastrutture remote, con supporto integrato per Tailscale e una dashboard di telemetria in tempo reale.

![App Header](app/src/main/res/drawable/img_start_banner_1779391997906.png)

## 🚀 Caratteristiche Principali

- **Gestore Profili Server**: Configura e salva i tuoi nodi SSH con parametri personalizzati (Host, Porta, Username).
- **Integrazione Tailscale**: Rilevamento automatico e supporto ottimizzato per le reti Tailscale (MagicDNS e IP 100.x.x.x).
- **Estetica Cyberpunk**: Interfaccia UI reattiva con effetti scanline CRT, animazioni neon e font monospazio.
- **Terminale Avanzato**: Shell interattiva con scorciatoie rapide per i comandi più comuni (`ls`, `htop`, `neofetch`, ecc.).
- **SFTP File Explorer**: Naviga, scarica, modifica ed elimina file sul server remoto tramite un'interfaccia grafica integrata.
- **Key Manager (RSA Keyring)**: Genera e gestisci coppie di chiavi RSA-2048 per un'autenticazione sicura senza password.
- **Monitoraggio Telemetria**: Visualizzazione in tempo reale di CPU, RAM, temperatura e traffico di rete tramite grafici ad oscilloscopio.
- **Widget Monitor**: Widget per la Home Screen che mostra lo stato dei tuoi server preferiti a colpo d'occhio.

## 🛠️ Tecnologie Utilizzate

- **Kotlin & Jetpack Compose**: UI moderna e dichiarativa.
- **Room Database**: Persistenza locale sicura per profili e chiavi.
- **Coroutines & Flow**: Gestione asincrona delle sessioni e della telemetria.
- **Material 3**: Componenti UI con personalizzazioni "Cyber-Theme".
- **Retrofit & Moshi**: Predisposto per integrazioni API esterne.

## 📱 Installazione e Build

### Requisiti
- Android 7.0 (API 24) o superiore.
- Android Studio Ladybug (o versioni recenti).
- Gradle 9.3.1.

### Build da Terminale
Per generare l'APK di release:
```bash
./gradlew assembleRelease
```
L'APK verrà generato in: `app/build/outputs/apk/release/app-release.apk`

## 🔒 Sicurezza
CoreShell supporta l'autenticazione tramite password e chiavi RSA. Le chiavi private sono memorizzate localmente nel database criptato dell'app. Si consiglia di utilizzare Tailscale per un ulteriore livello di sicurezza di rete.

## 📝 Note sulla Versione Corrente
*   La versione attuale utilizza un motore di simulazione per le sessioni SSH/SFTP e la telemetria.
*   L'integrazione con librerie SSH reali (come JSch) è prevista per le prossime iterazioni.

---
*Developed for the underground netrunner community.*
