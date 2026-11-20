package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.diagnosticDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "acopos_diagnostic_prefs"
)

data class DiagnosticPreferences(
    val bookmarkedCodes: Set<Int> = emptySet(),
    val recentCodes: List<Int> = emptyList(),
    val isDarkTheme: Boolean = true
)

class DiagnosticPreferencesRepository(private val context: Context) {

    private object Keys {
        val BOOKMARKS = stringPreferencesKey("bookmarked_codes")
        val RECENTS = stringPreferencesKey("recent_codes")
        val DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    val preferencesFlow: Flow<DiagnosticPreferences> = context.diagnosticDataStore.data.map { prefs ->
        val bookmarks = prefs[Keys.BOOKMARKS]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            ?: emptySet()

        val recents = prefs[Keys.RECENTS]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: emptyList()

        val isDark = prefs[Keys.DARK_THEME] ?: true

        DiagnosticPreferences(
            bookmarkedCodes = bookmarks,
            recentCodes = recents,
            isDarkTheme = isDark
        )
    }

    suspend fun toggleBookmark(code: Int) {
        context.diagnosticDataStore.edit { prefs ->
            val current = prefs[Keys.BOOKMARKS]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toMutableSet()
                ?: mutableSetOf()
            if (!current.add(code)) {
                current.remove(code)
            }
            prefs[Keys.BOOKMARKS] = current.joinToString(",")
        }
    }

    suspend fun recordRecent(code: Int) {
        context.diagnosticDataStore.edit { prefs ->
            val current = prefs[Keys.RECENTS]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toMutableList()
                ?: mutableListOf()
            current.remove(code)
            current.add(0, code)
            val trimmed = current.take(20)
            prefs[Keys.RECENTS] = trimmed.joinToString(",")
        }
    }

    suspend fun clearRecents() {
        context.diagnosticDataStore.edit { prefs ->
            prefs[Keys.RECENTS] = ""
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.diagnosticDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = isDark
        }
    }
}
