package cdglacier.mytool.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "widget_prefs")

interface WidgetConfigRepository {
    fun journalDirUri(widgetId: Int): Flow<Uri?>
    fun filenameFormat(widgetId: Int): Flow<String>
    fun vaultDirUri(widgetId: Int): Flow<Uri?>
    fun backgroundOpacity(widgetId: Int): Flow<Int>

    /**
     * null = キー未存在（このウィジェットでは未設定。フォールバックで全カレンダー表示）。
     * 空 set = ユーザーが明示的に 0 件を選択した状態（イベントは何も表示されない）。
     */
    fun selectedCalendarIds(widgetId: Int): Flow<Set<Long>?>

    suspend fun setJournalDirUri(widgetId: Int, uri: Uri)
    suspend fun setFilenameFormat(widgetId: Int, format: String)
    suspend fun setVaultDirUri(widgetId: Int, uri: Uri)
    suspend fun setBackgroundOpacity(widgetId: Int, opacity: Int)
    suspend fun setSelectedCalendarIds(widgetId: Int, ids: Set<Long>)

    suspend fun delete(widgetId: Int)

    fun getVaultName(vaultDirUri: Uri): String?

    companion object {
        const val DEFAULT_FILENAME_FORMAT = "yyyy-MM-dd"
        const val DEFAULT_BACKGROUND_OPACITY = 80
    }
}

@Singleton
class WidgetConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetConfigRepository {

    private fun journalDirUriKey(id: Int) = stringPreferencesKey("widget_${id}_journal_dir_uri")
    private fun filenameFormatKey(id: Int) = stringPreferencesKey("widget_${id}_filename_format")
    private fun vaultDirUriKey(id: Int) = stringPreferencesKey("widget_${id}_vault_dir_uri")
    private fun backgroundOpacityKey(id: Int) = intPreferencesKey("widget_${id}_background_opacity")
    private fun selectedCalendarIdsKey(id: Int) =
        stringSetPreferencesKey("widget_${id}_selected_calendar_ids")

    private val dataStore get() = context.widgetDataStore

    override fun journalDirUri(widgetId: Int): Flow<Uri?> =
        dataStore.data.map { it[journalDirUriKey(widgetId)]?.let(Uri::parse) }

    override fun filenameFormat(widgetId: Int): Flow<String> =
        dataStore.data.map {
            it[filenameFormatKey(widgetId)] ?: WidgetConfigRepository.DEFAULT_FILENAME_FORMAT
        }

    override fun vaultDirUri(widgetId: Int): Flow<Uri?> =
        dataStore.data.map { it[vaultDirUriKey(widgetId)]?.let(Uri::parse) }

    override fun backgroundOpacity(widgetId: Int): Flow<Int> =
        dataStore.data.map {
            it[backgroundOpacityKey(widgetId)] ?: WidgetConfigRepository.DEFAULT_BACKGROUND_OPACITY
        }

    override fun selectedCalendarIds(widgetId: Int): Flow<Set<Long>?> =
        dataStore.data.map { prefs ->
            prefs[selectedCalendarIdsKey(widgetId)]?.mapNotNull { it.toLongOrNull() }?.toSet()
        }

    override suspend fun setJournalDirUri(widgetId: Int, uri: Uri) {
        dataStore.edit { it[journalDirUriKey(widgetId)] = uri.toString() }
    }

    override suspend fun setFilenameFormat(widgetId: Int, format: String) {
        dataStore.edit { it[filenameFormatKey(widgetId)] = format }
    }

    override suspend fun setVaultDirUri(widgetId: Int, uri: Uri) {
        dataStore.edit { it[vaultDirUriKey(widgetId)] = uri.toString() }
    }

    override suspend fun setBackgroundOpacity(widgetId: Int, opacity: Int) {
        dataStore.edit { it[backgroundOpacityKey(widgetId)] = opacity }
    }

    override suspend fun setSelectedCalendarIds(widgetId: Int, ids: Set<Long>) {
        dataStore.edit { it[selectedCalendarIdsKey(widgetId)] = ids.map { id -> id.toString() }.toSet() }
    }

    override suspend fun delete(widgetId: Int) {
        dataStore.edit { prefs ->
            prefs.remove(journalDirUriKey(widgetId))
            prefs.remove(filenameFormatKey(widgetId))
            prefs.remove(vaultDirUriKey(widgetId))
            prefs.remove(backgroundOpacityKey(widgetId))
            prefs.remove(selectedCalendarIdsKey(widgetId))
        }
    }

    override fun getVaultName(vaultDirUri: Uri): String? =
        DocumentFile.fromTreeUri(context, vaultDirUri)?.name
}
