# GitHub Actions Release Automation Guide

## 🚀 Automated Release Workflow

The GitHub Actions workflow automatically creates releases when you push version tags or trigger it manually.

## 📋 How to Use

### Method 1: Tag-based Release (Recommended)

1. **Commit all changes** to your repository
2. **Create and push a version tag**:
   ```bash
   git tag v1.0.3
   git push origin v1.0.3
   ```
3. **GitHub Actions will automatically**:
   - Build the release APK
   - Create a GitHub release
   - Upload the APK as a release asset
   - Include release notes if available

### Method 2: Manual Trigger

1. Go to your GitHub repository
2. Click **Actions** tab
3. Select **Create Release** workflow
4. Click **Run workflow**
5. Enter the version (e.g., `v1.0.3`)
6. Click **Run workflow**

## 🔧 Workflow Features

### ✅ What it does automatically:
- **Builds Release APK** using Gradle
- **Extracts version** from git tag
- **Renames APK** to `DemantiaClockX-v1.0.3-release.apk`
- **Reads release notes** from `RELEASE_NOTES_v1.0.3.md`
- **Creates GitHub release** with proper title
- **Uploads APK** as release asset
- **Uploads release notes** as additional asset

### 🎯 Triggers:
- **Tag push**: Any tag starting with `v` (e.g., `v1.0.3`, `v2.0.0`)
- **Manual dispatch**: Can be triggered manually from GitHub Actions

### 📁 Required Files:
- `RELEASE_NOTES_v1.0.3.md` (optional, but recommended)
- Android project with `gradlew` script

## 🧪 Testing the Workflow

### For v1.0.3 Release:

1. **Push current changes**:
   ```bash
   git add .
   git commit -m "Add GitHub Actions release workflow"
   git push origin main
   ```

2. **Create and push tag**:
   ```bash
   git tag v1.0.3
   git push origin v1.0.3
   ```

3. **Monitor the workflow**:
   - Go to GitHub → Actions tab
   - Watch the "Create Release" workflow run
   - Check for any errors in the logs

4. **Verify the release**:
   - Go to GitHub → Releases tab
   - Confirm v1.0.3 release was created
   - Download and test the APK

## 🔍 Workflow Steps Explained

1. **Checkout**: Downloads repository code
2. **Setup Java 17**: Required for Android builds
3. **Setup Android SDK**: Android build tools
4. **Cache Gradle**: Speeds up builds
5. **Extract Version**: Gets version from tag/input
6. **Build APK**: Runs `./gradlew assembleRelease`
7. **Rename APK**: Gives it a proper name
8. **Read Release Notes**: Loads markdown content
9. **Create Release**: Creates GitHub release
10. **Upload Assets**: Adds APK and notes

## 🛠️ Customization Options

### Change APK Name Format:
Edit line in workflow:
```yaml
mv app/build/outputs/apk/release/app-release-unsigned.apk \
   app/build/outputs/apk/release/YourAppName-${{ steps.version.outputs.VERSION }}-release.apk
```

### Add Signing:
Add signing configuration before build step:
```yaml
- name: Setup Keystore
  run: |
    echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > keystore.jks
    
- name: Build Signed APK
  run: ./gradlew assembleRelease
  env:
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

### Change Release Notes Format:
Modify the release notes reading step to use different file naming or content.

## 🚨 Troubleshooting

### Common Issues:

1. **Build fails**: Check Android SDK version compatibility
2. **Permission denied**: Ensure `gradlew` has execute permissions
3. **Release notes not found**: File name must match `RELEASE_NOTES_v1.0.3.md`
4. **Tag already exists**: Delete and recreate tag if needed

### Debug Commands:
```bash
# Check existing tags
git tag -l

# Delete a tag locally and remotely
git tag -d v1.0.3
git push origin :refs/tags/v1.0.3

# View workflow logs
# Go to GitHub → Actions → Select workflow run → View logs
```

## 📈 Next Steps

1. **Test the workflow** with v1.0.3
2. **Monitor the build** for any issues
3. **Verify the release** is created correctly
4. **Update version** for next release
5. **Repeat the process** for future versions

---

**Created**: January 2025  
**Workflow File**: `.github/workflows/release.yml`  
**Current Version**: v1.0.3