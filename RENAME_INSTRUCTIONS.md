# Project Rename Instructions: academia → afora

## Overview
This document explains how to rename the entire AFORA project from "academia" to "afora" package names.

---

## What Will Be Changed

### Package Names
- `com.afora.android` → `com.afora.android`
- `com.afora.shared` → `com.afora.shared`
- `com.afora.backend` → `com.afora.backend`

### Class Names
- `AforaApplication` → `AforaApplication`
- `AforaBackendApplication` → `AforaBackendApplication`
- `AforaTheme` → `AforaTheme`

### Directory Structure
- All `academia` folders → `afora` folders
- File names containing "Academia" → "Afora"

### Configuration Files
- `build.gradle.kts` (namespace, applicationId)
- `AndroidManifest.xml`
- Backend configuration files
- All imports and references

---

## Prerequisites

1. **Close Android Studio** (important!)
2. **Commit your current work** to git:
   ```bash
   git add .
   git commit -m "Before renaming academia to afora"
   ```
3. **Ensure you're in the project root directory**

---

## Running the Rename Script

### Step 1: Open PowerShell
- Press `Win + X` and select "Windows PowerShell" or "Terminal"
- Navigate to your project root:
  ```powershell
  cd C:\Users\piyus\Desktop\Android_Projects\AFORA
  ```

### Step 2: Set Execution Policy (if needed)
If you get an execution policy error, run:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
```

### Step 3: Run the Script
```powershell
.\rename-academia-to-afora.ps1
```

### Step 4: Wait for Completion
The script will:
1. Create a backup (in `backup_before_rename_YYYYMMDD_HHMMSS` folder)
2. Update all file contents
3. Rename directories
4. Rename files
5. Show a summary

**Expected time:** 1-2 minutes depending on project size

---

## After Running the Script

### Step 1: Review Changes
```powershell
git status
git diff
```

Look for any unexpected changes.

### Step 2: Clean Build
```powershell
.\gradlew clean
```

### Step 3: Rebuild Project
```powershell
.\gradlew build
```

### Step 4: Run Tests
```powershell
.\gradlew test
```

### Step 5: Open in Android Studio
- Open Android Studio
- Click "Sync Project with Gradle Files"
- Check for any errors

---

## Verification Checklist

- [ ] Build completes without errors
- [ ] Tests pass
- [ ] Android app runs
- [ ] Backend starts
- [ ] No import errors in IDE
- [ ] Firebase configuration still works
- [ ] Git shows expected changes only

---

## If Something Goes Wrong

### Restore from Backup
The script creates a timestamped backup. To restore:

1. Find the backup folder: `backup_before_rename_YYYYMMDD_HHMMSS`
2. Copy the backed-up files back to their original locations
3. Or use git:
   ```bash
   git reset --hard HEAD
   git clean -fd
   ```

### Common Issues

**Issue 1: Build errors about missing classes**
- Solution: Clean and rebuild
  ```powershell
  .\gradlew clean build
  ```

**Issue 2: IDE shows red imports**
- Solution: Invalidate caches in Android Studio
  - File → Invalidate Caches → Invalidate and Restart

**Issue 3: Firebase configuration issues**
- Solution: Check `google-services.json` and ensure package name matches

**Issue 4: Some files still reference "academia"**
- Solution: Search for remaining references:
  ```powershell
  Get-ChildItem -Recurse -Include *.kt,*.xml,*.gradle* | Select-String "academia" -SimpleMatch
  ```

---

## Manual Verification Steps

After the script completes, manually check these files:

1. **AndroidManifest.xml**
   - Verify package name is `com.afora.android`

2. **build.gradle.kts (androidApp)**
   - Verify `namespace = "com.afora.android"`
   - Verify `applicationId = "com.afora.android"`

3. **google-services.json**
   - You may need to download a new one from Firebase Console with the new package name
   - Or update the `package_name` field inside the JSON

4. **Backend Application Class**
   - Verify it's renamed to `AforaBackendApplication`

5. **Main Activity**
   - Verify package is `com.afora.android`

---

## Updating Firebase (if needed)

If you need to update the package name in Firebase:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings → General
4. Under "Your apps", find your Android app
5. Update the package name to `com.afora.android`
6. Download the new `google-services.json`
7. Replace the old file in `androidApp/google-services.json`

---

## Git Commit

Once everything works:

```bash
git add .
git commit -m "Rename project from academia to afora

- Renamed all packages: com.academia.* → com.afora.*
- Renamed class names: Academia* → Afora*
- Updated directory structure
- Updated all configuration files
- Updated all imports and references
"
```

---

## Rollback Plan

If you need to completely rollback:

```bash
# Reset all changes
git reset --hard HEAD

# Remove any untracked files
git clean -fd

# Clean build
.\gradlew clean
```

---

## Notes

- The script preserves the `AFORA` project folder name (already correct)
- Backup is created automatically before any changes
- The script is idempotent (safe to run multiple times)
- Excludes build directories and IDE files from processing

---

## Support

If you encounter issues:
1. Check the backup folder
2. Review git diff
3. Try manual search and replace for missed items
4. Restore from backup if needed
