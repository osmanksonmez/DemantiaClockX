# DemantiaClockX 🕐

A specialized Android clock application designed for dementia patients and elderly users, featuring large, clear displays and automatic startup functionality.

## 📱 Features

### 🕐 Dual Clock Modes
- **Digital Clock**: Large, easy-to-read digital time display
- **Analog Clock**: Custom-designed analog clock with clear hour markers
- **One-tap switching** between digital and analog modes

### 🎨 Multiple Themes
- **Light Themes**: White Gray, Light Blue, Light Yellow, Light Pink, Red
- **Dark Themes**: Navy Blue, Black
- **High contrast** text for better visibility
- **Automatic theme persistence** across app restarts

### 📅 Comprehensive Time Display
- **Large time display** with AM/PM indicator
- **Full date information** including day of the week
- **Time of day indicators** (Morning, Noon, Afternoon, Evening, Night) in Turkish
- **Responsive layout** for different screen sizes and orientations

### 🔄 Auto-Start & Persistence
- **Automatic startup** on device boot
- **Foreground service** ensures app stays active
- **Battery optimization bypass** for uninterrupted operation
- **Multi-device compatibility** (Samsung, Xiaomi, Huawei, HTC support)

### 📲 Auto-Update System
- **Automatic update checking** from GitHub releases
- **Background download** and installation
- **User notifications** for available updates
- **One-tap update installation**

### 📱 Device Compatibility
- **Phone and tablet support** with responsive layouts
- **Portrait and landscape orientations**
- **Android 7.0+** compatibility
- **Fullscreen immersive mode** for distraction-free viewing

## 🚀 Installation

### Option 1: Download APK (Recommended)
1. Go to [Releases](https://github.com/yourusername/DemantiaClockX/releases)
2. Download the latest `DemantiaClockX-vX.X.X-release.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK file

### Option 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/yourusername/DemantiaClockX.git
cd DemantiaClockX

# Build the project
./gradlew clean assembleRelease

# APK will be generated in app/build/outputs/apk/release/
```

## 🛠️ Technical Details

### Architecture
- **Language**: Kotlin
- **UI Framework**: Android View System with Material Design 3
- **Build System**: Gradle with Kotlin DSL
- **Minimum SDK**: Android 24 (Android 7.0)
- **Target SDK**: Android 34

### Key Components
- **MainActivity**: Main clock display with theme management
- **SettingsActivity**: Theme selection and app configuration
- **CustomAnalogClock**: Custom-drawn analog clock view
- **AutoStartService**: Foreground service for auto-start functionality
- **BootReceiver**: Broadcast receiver for device boot detection
- **UpdateManager**: Automatic update system with GitHub integration

### Permissions
- `RECEIVE_BOOT_COMPLETED`: Auto-start on device boot
- `SYSTEM_ALERT_WINDOW`: Overlay permissions
- `WAKE_LOCK`: Keep device awake
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Battery optimization bypass
- `FOREGROUND_SERVICE`: Background service operation
- `POST_NOTIFICATIONS`: Update notifications (Android 13+)
- `INTERNET` & `ACCESS_NETWORK_STATE`: Update checking
- `REQUEST_INSTALL_PACKAGES`: APK installation

### Project Structure
```
app/src/main/
├── java/com/example/demantiaclockx/
│   ├── MainActivity.kt              # Main clock interface
│   ├── SettingsActivity.kt          # Settings and theme selection
│   ├── CustomAnalogClock.kt         # Custom analog clock view
│   ├── AutoStartService.kt          # Auto-start service
│   ├── BootReceiver.kt              # Boot detection
│   └── update/
│       ├── UpdateManager.kt         # Update system
│       └── UpdateBroadcastReceiver.kt
├── res/
│   ├── layout/                      # UI layouts for different screen sizes
│   ├── layout-land/                 # Landscape layouts
│   ├── layout-sw600dp/              # Tablet layouts
│   ├── values/                      # Colors, themes, strings
│   └── drawable/                    # App icons and graphics
└── AndroidManifest.xml              # App configuration and permissions
```

## 🎯 Target Audience

This application is specifically designed for:
- **Dementia patients** who need clear, simple time displays
- **Elderly users** who prefer large, easy-to-read interfaces
- **Caregivers** who need reliable, always-on clock displays
- **Care facilities** requiring consistent time display devices

## 🔧 Configuration

### First Launch Setup
1. **Grant permissions** when prompted (battery optimization, notifications)
2. **Select preferred theme** from the settings menu
3. **Choose clock mode** (digital or analog)
4. **Enable auto-start** for continuous operation

### Theme Customization
- Access settings via the gear icon
- Choose from 7 available themes
- Themes automatically apply to both clock modes
- Settings persist across app restarts

## 🔄 Auto-Update System

The app includes a sophisticated auto-update system:
- **Checks for updates** on app startup
- **Downloads updates** in the background
- **Notifies users** when updates are available
- **Supports one-tap installation** of new versions
- **Integrates with GitHub Releases** for version management

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Bug Reports & Feature Requests

Please use the [GitHub Issues](https://github.com/yourusername/DemantiaClockX/issues) page to:
- Report bugs
- Request new features
- Ask questions about usage
- Provide feedback

## 📞 Support

For support and questions:
- **GitHub Issues**: [Create an issue](https://github.com/yourusername/DemantiaClockX/issues)
- **Email**: your.email@example.com

## 🏆 Acknowledgments

- Designed with input from dementia care professionals
- Tested with elderly users for usability
- Built with accessibility and simplicity as core principles

---

**Made with ❤️ for dementia patients and their caregivers**