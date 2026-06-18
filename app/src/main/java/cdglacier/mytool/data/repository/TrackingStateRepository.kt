package cdglacier.mytool.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.trackingDataStore: DataStore<Preferences> by preferencesDataStore(name = "tracking_prefs")

enum class TrackingMode { STATIONARY, MOVING }

interface TrackingStateRepository {
    val trackingEnabled: Flow<Boolean>
    val mode: Flow<TrackingMode>
    suspend fun setTrackingEnabled(enabled: Boolean)
    suspend fun setMode(mode: TrackingMode)
    suspend fun getLastSignificantMoveAt(): Long
    suspend fun setLastSignificantMoveAt(epochMillis: Long)
    suspend fun isTrackingEnabledNow(): Boolean
    suspend fun getModeNow(): TrackingMode
}

@Singleton
class TrackingStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TrackingStateRepository {

    private val ENABLED_KEY = booleanPreferencesKey("tracking_enabled")
    private val MODE_KEY = stringPreferencesKey("tracking_mode")
    private val LAST_MOVE_KEY = longPreferencesKey("last_significant_move_at")

    override val trackingEnabled: Flow<Boolean> =
        context.trackingDataStore.data.map { it[ENABLED_KEY] ?: false }

    override val mode: Flow<TrackingMode> =
        context.trackingDataStore.data.map {
            runCatching { TrackingMode.valueOf(it[MODE_KEY] ?: TrackingMode.STATIONARY.name) }
                .getOrDefault(TrackingMode.STATIONARY)
        }

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        context.trackingDataStore.edit { it[ENABLED_KEY] = enabled }
    }

    override suspend fun setMode(mode: TrackingMode) {
        context.trackingDataStore.edit { it[MODE_KEY] = mode.name }
    }

    override suspend fun getLastSignificantMoveAt(): Long =
        context.trackingDataStore.data.map { it[LAST_MOVE_KEY] ?: 0L }.first()

    override suspend fun setLastSignificantMoveAt(epochMillis: Long) {
        context.trackingDataStore.edit { it[LAST_MOVE_KEY] = epochMillis }
    }

    override suspend fun isTrackingEnabledNow(): Boolean = trackingEnabled.first()
    override suspend fun getModeNow(): TrackingMode = mode.first()
}
