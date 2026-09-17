# Firebase Configuration for Android App

## ⚠️ IMPORTANT: google-services.json Required

The Android app requires `google-services.json` to build and run with Firebase Authentication.

### Status: ❌ Not Configured Yet

The file `google-services.json` is **NOT** included in this repository for security reasons.

---

## How to Get google-services.json

### Step 1: Follow Main Setup Guide
Refer to **[FIREBASE_SETUP.md](../FIREBASE_SETUP.md)** in the project root for complete Firebase setup instructions.

### Step 2: Download from Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **AFORA** project
3. Click ⚙️ **Project Settings**
4. Scroll to **"Your apps"** section
5. Find the Android app (`com.academia.android`)
6. Click **"google-services.json"** download button

### Step 3: Place the File
```bash
# Copy the downloaded file to:
AFORA/androidApp/google-services.json
```

### Step 4: Verify
After placing the file, run:
```bash
./gradlew :androidApp:assembleDebug
```

Expected output:
```
✅ google-services.json found
✅ Firebase initialized
BUILD SUCCESSFUL
```

---

## File Location

```
AFORA/
├── androidApp/
│   ├── google-services.json          # ⬅️ Place downloaded file here
│   ├── google-services-example.json  # Reference/template only
│   └── README_FIREBASE.md            # This file
```

---

## Security Notes

✅ **DO:**
- Download fresh `google-services.json` from Firebase Console
- Keep the file in `androidApp/` directory
- The file is automatically gitignored

❌ **DON'T:**
- Commit `google-services.json` to git
- Share the file publicly
- Use the example file for real builds

---

## Troubleshooting

### "google-services.json missing" error
**Solution:** Download and place the file as described above.

### "Could not find google-services.json"
**Check:**
1. File is in `androidApp/` (not in a subdirectory)
2. File name is exactly `google-services.json` (lowercase)
3. File is valid JSON (open it to verify)

### "Package name mismatch"
**Check:**
- Firebase Console Android app package: `com.academia.android`
- androidApp/build.gradle.kts namespace: `com.academia.android`
- They must match exactly

---

## Alternative: Mock Configuration (Development Only)

If you want to build without Firebase (for layout testing only), you can temporarily:

1. Rename `google-services-example.json` to `google-services.json`
2. Build will succeed, but **Firebase features won't work**
3. ⚠️ **Don't use this for production or real testing**

---

## Next Steps

After adding `google-services.json`:

1. ✅ Sync Gradle
2. ✅ Build app: `./gradlew :androidApp:assembleDebug`
3. ✅ Implement Firebase auth in shared module (Task #10)
4. ✅ Test authentication flow (Task #11)

---

**Need help?** Refer to [FIREBASE_SETUP.md](../FIREBASE_SETUP.md) for detailed instructions.
