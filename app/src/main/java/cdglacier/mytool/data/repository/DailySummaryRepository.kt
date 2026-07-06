package cdglacier.mytool.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import cdglacier.mytool.data.db.DailySummaryDao
import cdglacier.mytool.data.db.DailySummaryEntity
import cdglacier.mytool.domain.model.DailySummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface DailySummaryRepository {
    val summaries: Flow<Map<LocalDate, DailySummary>>
    val lastSyncedAtEpochMillis: Flow<Long?>
    suspend fun getExistingDates(dates: List<LocalDate>): Set<LocalDate>
    suspend fun upsertAll(summaries: List<DailySummary>, syncedAtEpochMillis: Long)
}

@Singleton
class DailySummaryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DailySummaryDao,
) : DailySummaryRepository {

    private val SYNCED_AT_KEY = longPreferencesKey("daily_summary_synced_at")

    override val summaries: Flow<Map<LocalDate, DailySummary>> =
        dao.observeAll().map { list ->
            list.associate { e ->
                val date = LocalDate.parse(e.date)
                date to DailySummary(
                    date = date,
                    habitRate = e.habitRate,
                    distanceMeters = e.distanceMeters,
                    householdHusbandPoints = e.householdHusbandPoints,
                )
            }
        }

    override val lastSyncedAtEpochMillis: Flow<Long?> =
        context.obsidianDataStore.data.map { it[SYNCED_AT_KEY] }

    override suspend fun getExistingDates(dates: List<LocalDate>): Set<LocalDate> {
        if (dates.isEmpty()) return emptySet()
        val strs = dates.map { it.toString() }
        return dao.getExistingDates(strs).map { LocalDate.parse(it) }.toSet()
    }

    override suspend fun upsertAll(summaries: List<DailySummary>, syncedAtEpochMillis: Long) {
        if (summaries.isEmpty()) return
        dao.upsertAll(summaries.map { s ->
            DailySummaryEntity(
                date = s.date.toString(),
                habitRate = s.habitRate,
                distanceMeters = s.distanceMeters,
                householdHusbandPoints = s.householdHusbandPoints,
            )
        })
        context.obsidianDataStore.edit { it[SYNCED_AT_KEY] = syncedAtEpochMillis }
    }
}
