package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_app_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        const val DEFAULT_GITHUB_REPO_SLUG = "sdm0023/mobile-shop-repair-manager"
        private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_GITHUB_REPO_SLUG = stringPreferencesKey("github_repo_slug")
    }

    val themeMode: Flow<AppThemeMode> = context.appPreferencesDataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: AppThemeMode.DARK.name
        runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.DARK)
    }

    val notificationsEnabled: Flow<Boolean> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val githubRepoSlug: Flow<String> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[KEY_GITHUB_REPO_SLUG]?.takeIf { it.isNotBlank() } ?: DEFAULT_GITHUB_REPO_SLUG
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setGithubRepoSlug(repoSlug: String) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[KEY_GITHUB_REPO_SLUG] = repoSlug
        }
    }
}
