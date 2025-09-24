# DemantiaClockX v1.0.9 Release Notes

## 🔧 Bug Fixes

### Screen Orientation Layout Fix
- **Fixed UI layout issues when rotating device between portrait and landscape modes**
  - Removed `configChanges` attribute from AndroidManifest.xml to allow proper Activity recreation
  - This ensures that the correct layout resources are loaded for each screen orientation
  - UI elements now properly resize and reposition when switching between orientations
  - Fixed text size inconsistencies between portrait, landscape, and tablet layouts

## 📱 Technical Changes

- Removed `android:configChanges="orientation|screenSize|keyboardHidden"` from MainActivity
- Activity now properly recreates on orientation changes, ensuring correct layout selection
- Improved compatibility with different screen sizes and orientations

## 🎯 What's Fixed

- Clock display now maintains proper proportions in all orientations
- Button layouts correctly adjust to screen orientation changes
- Text elements properly scale according to the selected layout configuration
- Smooth transitions between portrait and landscape modes

## 📋 Compatibility

- **Minimum Android Version:** Android 5.0 (API 21)
- **Target Android Version:** Android 14 (API 36)
- **Supported Orientations:** Portrait, Landscape
- **Supported Devices:** Phones and Tablets

---

**Release Date:** January 2025  
**Version Code:** 9  
**APK Size:** ~2.5 MB