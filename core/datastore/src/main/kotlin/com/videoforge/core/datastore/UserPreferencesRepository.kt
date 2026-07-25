package com.videoforge.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val RECENT_FILES_LIMIT = intPreferencesKey("recent_files_limit")
        val ENABLED_PLUGINS = stringPreferencesKey("enabled_plugins")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val OUTPUT_TREE_URI = stringPreferencesKey("output_tree_uri")
    }

    val recentFilesLimit: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[Keys.RECENT_FILES_LIMIT] ?: DEFAULT_RECENT_FILES_LIMIT
        }
        .catch {
            emit(DEFAULT_RECENT_FILES_LIMIT)
        }

    val enabledPluginIds: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            val raw = preferences[Keys.ENABLED_PLUGINS]

            if (raw == null) {
                DEFAULT_ENABLED_PLUGINS
            } else {
                raw.split(",")
                    .filter { it.isNotBlank() }
                    .toSet()
            }
        }
        .catch {
            emit(DEFAULT_ENABLED_PLUGINS)
        }

    val highContrast: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[Keys.HIGH_CONTRAST] ?: false
        }
        .catch {
            emit(false)
        }

    val outputTreeUri: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[Keys.OUTPUT_TREE_URI]
        }
        .catch {
            emit(null)
        }

    suspend fun setRecentFilesLimit(limit: Int) {
        val safeLimit = limit.coerceIn(1, 200)

        dataStore.edit { preferences ->
            preferences[Keys.RECENT_FILES_LIMIT] = safeLimit
        }
    }

    suspend fun setEnabledPluginIds(ids: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.ENABLED_PLUGINS] = ids.joinToString(",")
        }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.HIGH_CONTRAST] = enabled
        }
    }

    suspend fun setOutputTreeUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(Keys.OUTPUT_TREE_URI)
            } else {
                preferences[Keys.OUTPUT_TREE_URI] = uri
            }
        }
    }

    companion object {
        const val DEFAULT_RECENT_FILES_LIMIT = 50

        val DEFAULT_ENABLED_PLUGINS: Set<String> = setOf(
            "builtin.analysis.info"
        )
    }
}