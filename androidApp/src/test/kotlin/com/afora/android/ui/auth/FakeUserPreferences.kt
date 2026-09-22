package com.afora.android.ui.auth

import com.afora.android.data.CachedUser
import com.afora.android.data.UserPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory implementation of UserPreferencesStore for unit tests.
 * No Android Context or DataStore involved.
 */
class FakeUserPreferences : UserPreferencesStore {

    private var savedRole: String? = null
    private var savedUid: String?  = null
    private var savedEmail: String? = null
    private var savedName: String?  = null

    override suspend fun saveUser(
        firebaseUid: String,
        email: String,
        displayName: String,
        role: String
    ) {
        savedUid   = firebaseUid
        savedEmail = email
        savedName  = displayName
        savedRole  = role.uppercase()
    }

    override suspend fun clear() {
        savedRole  = null
        savedUid   = null
        savedEmail = null
        savedName  = null
    }

    override suspend fun getRole(): String? = savedRole

    override val roleFlow: Flow<String?> get() = flowOf(savedRole)

    override val firebaseUidFlow: Flow<String?> get() = flowOf(savedUid)

    override val cachedUser: Flow<CachedUser?> get() = flowOf(
        if (savedUid == null) null
        else CachedUser(savedUid!!, savedEmail ?: "", savedName ?: "", savedRole ?: "STUDENT")
    )
}
