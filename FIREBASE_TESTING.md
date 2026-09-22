# Firebase Authentication Testing Guide

## ✅ Setup Status

- ✅ Firebase project: **afora-5f362**
- ✅ Backend service account key: `backend/src/main/resources/firebase-service-account.json`
- ✅ Android google-services.json: `androidApp/google-services.json`

---

## Quick Start Testing

### 1. Test Backend Firebase Initialization

```bash
cd backend
./gradlew bootRun
```

**Expected Output:**
```
✅ Firebase Admin SDK initialized successfully
Firebase project ID: afora-5f362
Started AforaBackendApplication
```

### 2. Test Backend Health Endpoint

```bash
curl http://localhost:8080/api/health
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "afora-backend",
    "version": "1.0.0-A1"
  }
}
```

### 3. Build Android App

```bash
./gradlew :androidApp:assembleDebug
```

**Expected:**
```
✅ google-services.json found
BUILD SUCCESSFUL
```

---

## Create Test Users in Firebase

### Option 1: Firebase Console (Easiest)

1. Go to https://console.firebase.google.com/project/afora-5f362
2. Click **Authentication** → **Users**
3. Click **Add user**
4. Create test users:

**Student User:**
- Email: `student@afora.edu`
- Password: `Test123!`

**Teacher User:**
- Email: `teacher@afora.edu`
- Password: `Test123!`

**Admin User:**
- Email: `admin@afora.edu`
- Password: `Test123!`

### Option 2: Firebase REST API

```bash
# Get your Web API Key from Firebase Console → Project Settings → Web API Key
API_KEY="YOUR_WEB_API_KEY"

# Create student user
curl -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@afora.edu",
    "password": "Test123!",
    "returnSecureToken": true
  }'
```

---

## Test Authentication Flow

### Step 1: Get Firebase ID Token

```bash
# Replace with your Web API Key
API_KEY="YOUR_WEB_API_KEY"

curl -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@afora.edu",
    "password": "Test123!",
    "returnSecureToken": true
  }'
```

**Response will include:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2...",
  "refreshToken": "...",
  "expiresIn": "3600"
}
```

### Step 2: Test Backend with Token

```bash
# Copy the idToken from above
TOKEN="eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2..."

# Test protected endpoint
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

**Expected (once user endpoint is implemented):**
- ✅ 200 OK with user data, or
- ✅ 401/403 if endpoint requires user to exist in database

---

## Setup Database

### 1. Start PostgreSQL

```bash
# Windows (if PostgreSQL service isn't running)
net start postgresql-x64-15

# Or start from Services app
```

### 2. Create Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Run commands:
CREATE DATABASE afora_dev;
CREATE USER afora_user WITH PASSWORD 'afora_pass';
GRANT ALL PRIVILEGES ON DATABASE afora_dev TO afora_user;
\q
```

### 3. Run Migrations

```bash
cd backend
./gradlew flywayMigrate
```

**Expected:**
```
Successfully applied 4 migrations
- V1: init_schema
- V2: seed_dev_data  
- V3: create_user_tables
- V4: seed_test_users
```

### 4. Verify Tables

```bash
psql -U afora_user -d afora_dev

# Check tables
\dt

# Check users
SELECT id, firebase_uid, email, role FROM users;
```

---

## Integration Test Checklist

- [ ] Backend starts with Firebase initialized
- [ ] Database migrations complete
- [ ] Android app builds successfully
- [ ] Firebase test user created
- [ ] Can get ID token from Firebase
- [ ] Backend validates Firebase token
- [ ] User data synced between Firebase and database

---

## Troubleshooting

### Backend: "Firebase service account key not found"

**Solution:**
```bash
# Check file exists
ls backend/src/main/resources/firebase-service-account.json

# Should show file with ~2KB size
```

### Android: "google-services.json missing"

**Solution:**
```bash
# Check file exists
ls androidApp/google-services.json

# File should contain "project_info" with "project_id": "afora-5f362"
```

### Backend: "Could not connect to PostgreSQL"

**Solution:**
```bash
# Check PostgreSQL is running
psql -U postgres -c "SELECT version();"

# Update backend/src/main/resources/application-dev.yml if needed
```

### Token validation fails

**Check:**
- Token hasn't expired (valid for 1 hour)
- Backend and Android use same Firebase project
- System clock is synchronized

---

## Next Steps

After successful testing:

1. ✅ Commit firebase config files (they're gitignored)
2. 🚀 Implement authentication UI screens
3. 🚀 Build login/register screens
4. 🚀 Create user profile management
5. 🚀 Implement role-based navigation

---

## Project Info

- **Firebase Project ID:** afora-5f362
- **Package Name:** com.afora.android
- **Backend Port:** 8080
- **Database:** afora_dev

---

**Firebase setup complete! 🎉**

Run the tests above to verify everything works.
