package com.imagetotext.converter.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Remembers the user's light/dark mode choice locally (DataStore is just a
 * small file on device storage), so it survives closing and reopening the
 * app. A null value means "no explicit choice yet" - the caller falls back
 * to the system theme in that case.
 */
class ThemePreferences(private val context: Context) {

    private val darkModeKey = booleanPreferencesKey("is_dark_mode")

    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[darkModeKey]
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }
}
