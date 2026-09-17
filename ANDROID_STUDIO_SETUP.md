# Building AFORA in Android Studio

## Quick Setup

### 1. Open Project in Android Studio

1. Launch Android Studio
2. Click **"Open"** or **File > Open**
3. Navigate to: `C:\Users\piyus\Desktop\AFORA`
4. Select the folder and click **OK**

### 2. Wait for Gradle Sync

Android Studio will automatically:
- ✅ Detect it's a Kotlin Multiplatform project
- ✅ Use the correct JDK (it has its own JDK 17)
- ✅ Download dependencies
- ✅ Sync Gradle

**This may take 3-5 minutes the first time.**

### 3. Build the Project

**Option A: Build Everything**
- Click **Build > Make Project** (Ctrl+F9)
- Or click the hammer icon 🔨 in the toolbar

**Option B: Build Specific Module**
- Right-click on `:shared` module → **Build Module**
- Right-click on `:androidApp` module → **Build Module**

### 4. Run the Android App

1. Create/start an Android emulator:
   - Click **Device Manager** (phone icon)
   - Create a new virtual device if needed
   - Start the emulator

2. Select `androidApp` run configuration (top toolbar)

3. Click the green ▶️ **Run** button

## Advantages of Using Android Studio

✅ **Better Error Messages**
- Inline error highlighting
- Clickable error links
- Detailed error explanations

✅ **Auto JDK Selection**
- Uses bundled JDK 17 automatically
- No manual JAVA_HOME configuration needed

✅ **Gradle Integration**
- Visual Gradle tool window
- Easy dependency management
- Build variants selector

✅ **Code Navigation**
- Jump to definitions
- Find usages
- Refactoring tools

✅ **Debugging**
- Breakpoints
- Variable inspection
- Step-through debugging

## Troubleshooting

### Gradle Sync Fails

**Solution 1: Invalidate Caches**
1. File > Invalidate Caches
2. Check "Clear downloaded shared indexes"
3. Click "Invalidate and Restart"

**Solution 2: Update Gradle**
1. File > Project Structure
2. Project Settings > Project
3. Set Gradle JDK to "Embedded JDK (17)"

### Cannot Find Android SDK

**Solution:**
1. File > Settings (Ctrl+Alt+S)
2. Appearance & Behavior > System Settings > Android SDK
3. Note the SDK Location
4. If empty, click "Edit" and install SDK

### Build Errors

**View Full Build Output:**
1. Click **Build** tab at bottom
2. Check detailed error messages
3. Click on errors to jump to source

**Clean and Rebuild:**
1. Build > Clean Project
2. Build > Rebuild Project

## Module Structure in Android Studio

```
AFORA
├── 📦 shared (KMP module)
│   ├── commonMain
│   ├── androidMain
│   └── iosMain
├── 📱 androidApp (Android app)
│   └── src/main
└── 🔧 backend (separate Gradle project)
```

## Build Configurations

**Debug Build:**
- Fast compilation
- Debugging enabled
- No obfuscation

**Release Build:**
- Optimized
- ProGuard enabled
- Signed APK

**Select Build Variant:**
- View > Tool Windows > Build Variants
- Choose `debug` or `release`

## Running Tests

**Run All Tests:**
1. Right-click on `test` folder
2. Select "Run 'Tests in...'"

**Run Single Test:**
1. Open test file
2. Click green arrow next to test function
3. Select "Run"

## Useful Shortcuts

| Action | Windows |
|--------|---------|
| Build Project | Ctrl+F9 |
| Run App | Shift+F10 |
| Debug App | Shift+F9 |
| Find Anything | Double Shift |
| Go to File | Ctrl+Shift+N |
| Recent Files | Ctrl+E |
| Code Completion | Ctrl+Space |

## Next Steps After Build

### 1. Fix Any Remaining Errors

Android Studio will show:
- Red squiggly lines for errors
- Yellow for warnings
- Hover over them for details

### 2. Run the App

- Select an emulator or physical device
- Click Run ▶️
- App should launch with "Phase A1: Foundation Complete" screen

### 3. Backend (Separate)

Backend is a separate project:
1. Open a **new** Android Studio window
2. Open `C:\Users\piyus\Desktop\AFORA\backend`
3. Build and run backend separately

Or use terminal:
```powershell
cd C:\Users\piyus\Desktop\AFORA\backend
.\gradlew bootRun
```

## Tips

### Multi-Module Project

- Use **Project** view (not Android view) to see all modules
- Each module has its own `build.gradle.kts`

### Kotlin Multiplatform

- `commonMain` = shared code
- `androidMain` = Android-specific code
- `iosMain` = iOS-specific (commented out for Phase A1)

### Performance

**Speed up builds:**
1. File > Settings > Build > Compiler
2. Enable "Configure on demand"
3. Set "Command-line Options": `--parallel`

### Code Style

Android Studio will format code automatically:
- Ctrl+Alt+L = Reformat code
- Ctrl+Alt+O = Optimize imports

## Expected Result

After successful build:

✅ No red errors in code editor
✅ Build tab shows "BUILD SUCCESSFUL"
✅ APK generated at: `androidApp/build/outputs/apk/debug/`
✅ Can run app on emulator/device
✅ App shows Material 3 theme with "Phase A1 Complete" screen

## Common Android Studio Features

### 1. Logcat

View app logs:
- View > Tool Windows > Logcat
- Filter by tag (e.g., "ApiClient")
- See debug/error messages

### 2. Device File Explorer

Browse app files on device:
- View > Tool Windows > Device File Explorer
- Navigate to `/data/data/com.academia.android/`

### 3. Layout Inspector

Inspect Compose UI:
- Tools > Layout Inspector
- Connect to running app
- See component hierarchy

### 4. Profiler

Monitor performance:
- View > Tool Windows > Profiler
- CPU, Memory, Network usage
- Identify bottlenecks

## Phase A1 Verification Checklist

After building in Android Studio:

- [ ] Gradle sync completes without errors
- [ ] All modules show green checkmark ✓
- [ ] No red errors in code editor
- [ ] Build succeeds (Ctrl+F9)
- [ ] APK generated successfully
- [ ] App runs on emulator
- [ ] Foundation screen displays correctly

## Need Help?

**Check Build Output:**
- Build tab shows detailed error messages
- Click on error to jump to source
- Read error description carefully

**Gradle Tool Window:**
- View > Tool Windows > Gradle
- Run specific Gradle tasks
- See all available tasks

**Event Log:**
- Bottom right corner notification icon
- Shows Gradle sync issues
- Clickable suggestions to fix

---

**You can now build the complete AFORA project in Android Studio!** 🎉

The IDE will handle JDK configuration, dependency resolution, and provide much better error messages than command-line builds.
