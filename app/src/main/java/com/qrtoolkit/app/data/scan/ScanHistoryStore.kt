package com.qrtoolkit.app.data.scan

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qrtoolkit.app.data.model.ScannedCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.scanHistoryDataStore by preferencesDataStore(name = "scan_history")

/** Persists the last [MAX_HISTORY] decoded barcodes, newest last. */
class ScanHistoryStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(ScannedCode.serializer())
    private val historyKey = stringPreferencesKey("history_json")

    val history: Flow<List<ScannedCode>> = context.scanHistoryDataStore.data.map { prefs ->
        decode(prefs[historyKey])
    }

    suspend fun add(scan: ScannedCode) {
        context.scanHistoryDataStore.edit { prefs ->
            val updated = (decode(prefs[historyKey]) + scan).takeLast(MAX_HISTORY)
            prefs[historyKey] = json.encodeToString(serializer, updated)
        }
    }

    suspend fun clear() {
        context.scanHistoryDataStore.edit { prefs -> prefs[historyKey] = json.encodeToString(serializer, emptyList()) }
    }

    private fun decode(raw: String?): List<ScannedCode> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val MAX_HISTORY = 100
    }
}
