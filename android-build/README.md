# Old School Forge Android App (v0.36.0)

This is a simple Android app that provides convenient access to the Old School Forge web interface.

## Building the APK

To build the APK from this source code:

1. Make sure you have Android Studio installed (version 2023.3.1 or newer).
2. Open this directory as a project in Android Studio.
3. Wait for Gradle sync to complete.
4. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
5. The built APK will be available in app/build/outputs/apk/debug/app-debug.apk.

## Troubleshooting Gradle Issues

If you encounter Gradle-related errors:

1. In Android Studio, go to File > Settings > Build, Execution, Deployment > Gradle
2. Make sure "Use Gradle from" is set to "Specified location" or "Gradle Wrapper"
3. Click "OK" and try building again
4. If issues persist, try File > Invalidate Caches / Restart

## Installation Instructions

There are two ways to install the app:

### Direct APK Installation
1. Download the APK file to your Android device.
2. Make sure you've enabled "Install from Unknown Sources" in your device settings.
3. Open the APK file and follow the prompts to install it.

### Using Android Studio
1. Connect your Android device to your computer via USB.
2. Enable USB debugging on your device.
3. In Android Studio, select Run > Run 'app'.
4. Select your device from the list.

## Requirements
- Android 8.0 (API level 26) or higher

## Features
- Provides direct access to Old School Forge web interface
- Fullscreen gaming experience
- Back button navigation support

## Contact
For any issues or questions, please report to the main project repository:
https://github.com/vanja-ivancevic/forge-old-school-shandalar 