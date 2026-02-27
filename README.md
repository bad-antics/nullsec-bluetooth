<!-- 
SEO Keywords: NullSec Bluetooth, Bluetooth security app, BLE scanner, Bluetooth hacking app,
device discovery, GATT explorer, Bluetooth pentesting, BLE security analyzer,
bad-antics, bad-antics, NullSec Framework, mobile Bluetooth tools, device tracker
-->

<div align="center">

# 🔵 NullSec Bluetooth

### Advanced Bluetooth Security Analyzer

[![X/Twitter](https://img.shields.io/badge/🔑_GET_KEYS-x.com/AnonAntics-5865F2?style=for-the-badge&logo=x&logoColor=white)](https://x.com/AnonAntics)
[![GitHub](https://img.shields.io/badge/GitHub-bad--antics-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/bad-antics)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/bad-antics/nullsec-bluetooth)

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)]()
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)]()

```
    ███▄    █  █    ██  ██▓     ██▓      ██████ ▓█████  ▄████▄  
    ██ ▀█   █  ██  ▓██▒▓██▒    ▓██▒    ▒██    ▒ ▓█   ▀ ▒██▀ ▀█  
   ▓██  ▀█ ██▒▓██  ▒██░▒██░    ▒██░    ░ ▓██▄   ▒███   ▒▓█    ▄ 
   ▓██▒  ▐▌██▒▓▓█  ░██░▒██░    ▒██░      ▒   ██▒▒▓█  ▄ ▒▓▓▄ ▄██▒
   ▒██░   ▓██░▒▒█████▓ ░██████▒░██████▒▒██████▒▒░▒████▒▒ ▓███▀ ░
   ░ ▒░   ▒ ▒ ░▒▓▒ ▒ ▒ ░ ▒░▓  ░░ ▒░▓  ░▒ ▒▓▒ ▒ ░░░ ▒░ ░░ ░▒ ▒  ░
     ░    ░    ░   ░   ░         ░            ░   ░   ░        
   ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
   █░░░░░░░░░░░░░░░ B L U E T O O T H ░░░░░░░░░░░░░░░░░░░░░░░░█
   ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
                       bad-antics
```

### 🔓 **[Join x.com/AnonAntics](https://x.com/AnonAntics)** for premium features!

</div>

---

## 🎯 Features

| Feature | Free | Premium |
|---------|:----:|:-------:|
| 🔍 Device Discovery | ✅ | ✅ |
| 📊 Signal Strength | ✅ | ✅ |
| 📱 Device Classification | ✅ | ✅ |
| 🏭 Manufacturer Lookup | ✅ | ✅ |
| 🔗 GATT Service Explorer | ❌ | ✅ |
| 📡 BLE Advertising Data | ❌ | ✅ |
| 🕵️ Device Tracking | ❌ | ✅ |
| 📈 Signal History | ❌ | ✅ |
| 📋 Export Reports | JSON | All formats |
| 🔔 Proximity Alerts | ❌ | ✅ |
| 🗺️ Device Mapping | ❌ | ✅ |

---

## 📱 Supported Devices

### Bluetooth Classic
- 📱 Smartphones & Tablets
- 💻 Laptops & Computers
- 🎧 Headphones & Earbuds
- 🔊 Speakers & Audio Devices
- 🖱️ Keyboards & Mice
- 🚗 Car Audio Systems
- 🎮 Game Controllers

### Bluetooth Low Energy (BLE)
- ⌚ Smartwatches & Fitness Trackers
- 🏥 Medical Devices
- 🏠 Smart Home Devices
- 🔒 Smart Locks
- 📍 Beacons & Trackers
- 🌡️ Sensors & IoT Devices

---

## 🛡️ Security Analysis

### Device Classification
| Type | Icon | Detection Method |
|------|------|------------------|
| Phone | 📱 | CoD + Name pattern |
| Computer | 💻 | CoD major class |
| Audio | 🎧 | CoD + A2DP service |
| Wearable | ⌚ | BLE services |
| IoT | 🔌 | BLE + unknown CoD |
| Beacon | 📍 | iBeacon/Eddystone |

### GATT Analysis (Premium)
- Service UUID enumeration
- Characteristic discovery
- Security property analysis
- Read/Write permissions check
- Notification capabilities

---

## 📦 Installation

### Android
```bash
# Download from releases or build:
git clone https://github.com/bad-antics/nullsec-bluetooth.git
cd nullsec-bluetooth/android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
