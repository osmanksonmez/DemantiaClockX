# DemantiaClockX v1.0.5 Release Notes

## 🔒 Security Fix Release
**Release Date:** January 25, 2025  
**Version Code:** 6  
**Version Name:** 1.0.5

## 🛡️ Critical Security Updates

### APK Security Verification Fix
- **Fixed:** APK security verification failure on Android API 28+ devices
- **Issue:** "APK bilgileri okunamadı" (APK information could not be read) error during app updates
- **Solution:** Updated PackageManager API usage for modern Android versions

### Technical Improvements
- **Updated API Compatibility:** 
  - Android API 28+: Now uses `PackageManager.GET_SIGNING_CERTIFICATES`
  - Android API < 28: Continues to use legacy `PackageManager.GET_SIGNATURES`
- **Enhanced Error Handling:** More detailed logging and error messages for debugging
- **Signature Verification:** Improved signature verification process with better multi-signer support

## 🔧 Technical Details

### Updated Methods
- `verifyApkSignature()`: Now handles both new and legacy Android APIs
- `getCurrentAppSignatures()`: Updated to use appropriate API based on Android version
- Enhanced null safety and error reporting

### Compatibility
- **Minimum SDK:** Android 5.0 (API 21)
- **Target SDK:** Android 14 (API 36)
- **Compile SDK:** Android 14 (API 36)

## 🚀 What's Fixed
- ✅ APK security verification now works on all Android versions
- ✅ App updates will no longer fail with security check errors
- ✅ Improved compatibility with modern Android security requirements
- ✅ Better error reporting for troubleshooting

## 📱 User Impact
- **Seamless Updates:** App updates will now work properly on all Android devices
- **Enhanced Security:** Maintains strong security while ensuring compatibility
- **No UI Changes:** This is a backend security fix with no visible changes to the user interface

## 🔄 Previous Versions
- **v1.0.4:** Added seconds display to clock, improved time precision
- **v1.0.3:** Enhanced UI themes and visual improvements
- **v1.0.2:** Performance optimizations and bug fixes
- **v1.0.1:** Initial feature enhancements

## 📋 Installation Notes
- This is a critical security update
- Recommended for all users to ensure proper app update functionality
- No data loss or settings reset required

---
**Build Information:**
- Build Type: Release
- Signed: Yes
- Optimized: Yes
- Security Patch: January 2025