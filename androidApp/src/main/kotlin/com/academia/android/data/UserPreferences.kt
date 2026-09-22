package com.academia.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Interface so tests can inject a fake without touching DataStore.
 */
interface UserPreferencesStore {
    suspend fun saveUser(firebaseUid: String, email: String, displayName: String, role: String)
    suspend fun clear()
    suspend fun getRole(): String?
    val roleFlow: Flow<String?>
    val firebaseUidFlow: Flow<String?>
    val cachedUser: Flow<CachedUser?>
}

/**
 * DataStore-backed preferences for the authenticated user.
 *
 * Persists the user's role, UID and display info locally so the app can
 * route to the correct dashboard on cold start without a backend round-trip.
 *
 * Source-of-truth ordering:
 *  1. Backend /api/auth/sync response  (written on every login/register)
 *  2. DataStore cache                  (read on app start)
 *  3. Firebase custom claim fallback   (if DataStore is empty)
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesStore {
    companion object {
        val KEY_FIREBASE_UID  = stringPreferencesKey("firebase_uid")
        val KEY_EMAIL         = stringPreferencesKey("email")
        val KEY_DISPLAY_NAME  = stringPreferencesKey("display_name")
        val KEY_ROLE          = stringPreferencesKey("role")
    }

    // ── Writes ────────────────────────────────────────────────────────────────

    override suspend fun saveUser(
        firebaseUid: String,
        email: String,
        displayName: String,
        role: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIREBASE_UID]  = firebaseUid
            prefs[KEY_EMAIL]         = email
            prefs[KEY_DISPLAY_NAME]  = displayName
            prefs[KEY_ROLE]          = role.uppercase()
        }
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    override val roleFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ROLE] }

    override suspend fun getRole(): String? = roleFlow.first()

    override val firebaseUidFlow: Flow<String?> = context.dataStore.data.map { it[KEY_FIREBASE_UID] }

    override val cachedUser: Flow<CachedUser?> = context.dataStore.data.map { prefs ->
        val uid = prefs[KEY_FIREBASE_UID] ?: return@map null
        CachedUser(
            firebaseUid = uid,
            email       = prefs[KEY_EMAIL]        ?: "",
            displayName = prefs[KEY_DISPLAY_NAME] ?: "",
            role        = prefs[KEY_ROLE]         ?: "STUDENT"
        )
    }
}

/** Lightweight cached representation of the signed-in user. */
data class CachedUser(
    val firebaseUid: String,
    val email: String,
    val displayName: String,
    val role: String
)
