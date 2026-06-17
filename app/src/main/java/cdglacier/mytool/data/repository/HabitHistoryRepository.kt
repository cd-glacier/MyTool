package cdglacier.mytool.data.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface HabitHistoryRepository {
    /** 日付ごとの達成率(0.0〜1.0)。該当する習慣が無い日はマップに含まれない。 */
    val history: Flow<Map<LocalDate, Float>>
    val lastSyncedAtEpochMillis: Flow<Long?>
    suspend fun save(rates: Map<LocalDate, Float>, syncedAtEpochMillis: Long)
}

@Singleton
class HabitHistoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HabitHistoryRepository {

    private val HISTORY_KEY = stringPreferencesKey("habit_history_json")
    private val SYNCED_AT_KEY = longPreferencesKey("habit_history_synced_at")

    override val history: Flow<Map<LocalDate, Float>> =
        context.obsidianDataStore.data.map { prefs ->
            val json = prefs[HISTORY_KEY] ?: return@map emptyMap()
            runCatching {
                Json.decodeFromString<Map<String, Float>>(json)
                    .mapKeys { LocalDate.parse(it.key) }
            }.getOrDefault(emptyMap())
        }

    override val lastSyncedAtEpochMillis: Flow<Long?> =
        context.obsidianDataStore.data.map { it[SYNCED_AT_KEY] }

    override suspend fun save(rates: Map<LocalDate, Float>, syncedAtEpochMillis: Long) {
        val serializable = rates.mapKeys { it.key.toString() }
        val json = Json.encodeToString<Map<String, Float>>(serializable)
        context.obsidianDataStore.edit { prefs ->
            prefs[HISTORY_KEY] = json
            prefs[SYNCED_AT_KEY] = syncedAtEpochMillis
        }
    }
}
