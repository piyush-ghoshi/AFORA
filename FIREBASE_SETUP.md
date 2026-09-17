# Firebase Setup Guide for AFORA

This guide walks you through setting up Firebase Authentication for the AFORA project.

---

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or **"Create a project"**
3. Enter project name: **AFORA** (or your preferred name)
4. Optional: Enable Google Analytics (recommended for production)
5. Click **"Create project"**

---

## Step 2: Enable Authentication

1. In Firebase Console, select your **AFORA** project
2. Click **"Authentication"** in the left sidebar
3. Click **"Get started"**
4. Go to **"Sign-in method"** tab
5. Enable the following sign-in providers:
   - ✅ **Email/Password** - Click, toggle "Enable", Save
   - Optional: **Google** (for future social login)

---

## Step 3: Add Android App

1. In Firebase Console, click the **Android icon** (⚙️ Project settings → Add app)
2. **Android package name:** `com.academia.android`
3. **App nickname:** AFORA Android (optional)
4. **Debug signing certificate SHA-1:** (optional for now, required for Google Sign-In)
   ```bash
   # To get SHA-1 for debug keystore:
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Click **"Register app"**
6. **Download `google-services.json`**
   - Save this file - we'll add it to the project in Task #9
7. Click **"Next"** → **"Next"** → **"Continue to console"**

---

## Step 4: Generate Service Account Key (Backend)

This key allows the Spring Boot backend to verify Firebase tokens.

1. In Firebase Console, click ⚙️ (Project Settings)
2. Go to **"Service accounts"** tab
3. Click **"Generate new private key"**
4. Click **"Generate key"** - a JSON file will download
5. **Rename the file to:** `firebase-service-account.json`
6. **Save location:** `backend/src/main/resources/firebase-service-account.json`
   
   ⚠️ **IMPORTANT:** Add this to `.gitignore` (already done in Phase A1)

---

## Step 5: Update Backend Configuration

Add Firebase service account path to `application.yml`:

```yaml
# backend/src/main/resources/application.yml
firebase:
  service-account-key: classpath:firebase-service-account.json
```

For different environments:

**Development (`application-dev.yml`):**
```yaml
firebase:
  service-account-key: classpath:firebase-service-account.json
```

**Production (`application-prod.yml`):**
```yaml
firebase:
  service-account-key: ${FIREBASE_SERVICE_ACCOUNT_KEY_PATH}
```

In production, set the environment variable:
```bash
export FIREBASE_SERVICE_ACCOUNT_KEY_PATH=/secure/path/to/firebase-service-account.json
```

---

## Step 6: Verify Setup

### Backend Verification

After implementing FirebaseConfig (Task #3), the backend should log:
```
Firebase Admin SDK initialized successfully
```

### Android Verification

After adding `google-services.json` (Task #9), the build should succeed with:
```
✅ google-services.json found
✅ Firebase initialized
```

---

## Security Best Practices

### ✅ DO:
- Store `firebase-service-account.json` securely
- Add to `.gitignore` (already configured)
- Use environment variables in production
- Rotate service account keys periodically
- Enable Firebase App Check for production

### ❌ DON'T:
- Commit service account JSON to git
- Share service account keys publicly
- Use the same project for dev and prod
- Hardcode credentials in code

---

## Firebase Authentication Flow (Overview)

### Android App:
1. User enters email/password
2. Firebase Authentication SDK authenticates
3. Firebase returns **ID Token** (JWT)
4. App sends ID Token to backend in `Authorization: Bearer <token>` header

### Backend:
1. Receives request with Firebase ID Token
2. Firebase Admin SDK verifies token
3. Extracts user info (UID, email, etc.)
4. Maps Firebase UID to local User entity
5. Returns protected resource

---

## Project Structure After Setup

```
AFORA/
├── backend/
│   └── src/main/resources/
│       ├── firebase-service-account.json   # ⚠️ NOT in git
│       ├── application.yml                 # References service account
│       └── application-dev.yml
├── androidApp/
│   └── google-services.json                # ⚠️ NOT in git (for now)
└── FIREBASE_SETUP.md                       # This file
```

---

## Troubleshooting

### "Service account key not found"
- Ensure `firebase-service-account.json` is in `backend/src/main/resources/`
- Check file name matches exactly
- Verify `application.yml` path is correct

### "Invalid Firebase token"
- Ensure Android app is using the same Firebase project
- Check `google-services.json` is correct
- Verify token hasn't expired (tokens are valid for 1 hour)

### "google-services.json not found"
- Ensure file is in `androidApp/` directory (root of Android module)
- Re-download from Firebase Console if needed
- Run Gradle sync after adding

---

## Next Steps

After Firebase setup:
- ✅ Task #3: Implement FirebaseConfig in backend
- ✅ Task #4: Create FirebaseAuthenticationFilter
- ✅ Task #8: Add Firebase dependencies to Android
- ✅ Task #9: Add google-services.json to Android
- ✅ Task #10: Implement auth in shared module

---

## Firebase Console URLs

- **Project Overview:** https://console.firebase.google.com/project/YOUR_PROJECT_ID
- **Authentication:** https://console.firebase.google.com/project/YOUR_PROJECT_ID/authentication
- **Users:** https://console.firebase.google.com/project/YOUR_PROJECT_ID/authentication/users
- **Service Accounts:** https://console.firebase.google.com/project/YOUR_PROJECT_ID/settings/serviceaccounts

---

**Once you've completed Steps 1-4 and downloaded the files, mark Task #2 as complete and proceed to Task #3!**
