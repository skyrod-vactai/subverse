package org.skyrod.subverse

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class HistoryStorage(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "history")
    }

    private val historyKey = stringPreferencesKey("history_list")
    suspend fun saveHistory(history: List<String>) {
        try {
            val value = history.joinToString(",")
            Log.d("HistoryStorage", "Saving history: $value")
            context.dataStore.edit { preferences ->
                preferences[historyKey] = value
            }
        } catch (e: Exception) {
            Log.e("HistoryStorage", "Error saving history", e)
        }
    }

    suspend fun loadHistory(): List<String> {
        return try {
            context.dataStore.data.map { preferences ->
                val value = preferences[historyKey]
                Log.d("HistoryStorage", "Loaded history: $value")
                value?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            }.first()
        } catch (e: Exception) {
            Log.e("HistoryStorage", "Error loading history", e)
            emptyList()
        }
    }

}