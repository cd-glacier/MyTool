package cdglacier.mytool.widget

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "widget_prefs")

object WidgetPreferences {
    const val DEFAULT_FILENAME_FORMAT = "yyyy-MM-dd"

    fun journalDirUriKey(id: Int) = stringPreferencesKey("widget_${id}_journal_dir_uri")
    fun filenameFormatKey(id: Int) = stringPreferencesKey("widget_${id}_filename_format")
    fun vaultDirUriKey(id: Int) = stringPreferencesKey("widget_${id}_vault_dir_uri")
    fun backgroundOpacityKey(id: Int) = intPreferencesKey("widget_${id}_background_opacity")
    fun selectedCalendarIdsKey(id: Int) = stringSetPreferencesKey("widget_${id}_selected_calendar_ids")

    fun getJournalDirUriFlow(context: Context, widgetId: Int): Flow<Uri?> =
        context.widgetDataStore.data.map { prefs ->
            prefs[journalDirUriKey(widgetId)]?.let { Uri.parse(it) }
        }

    suspend fun setJournalDirUri(context: Context, widgetId: Int, uri: Uri) {
        context.widgetDataStore.edit { prefs ->
            prefs[journalDirUriKey(widgetId)] = uri.toString()
        }
    }

    fun getFilenameFormatFlow(context: Context, widgetId: Int): Flow<String> =
        context.widgetDataStore.data.map { prefs ->
            prefs[filenameFormatKey(widgetId)] ?: DEFAULT_FILENAME_FORMAT
        }

    suspend fun setFilenameFormat(context: Context, widgetId: Int, format: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[filenameFormatKey(widgetId)] = format
        }
    }

    fun getVaultDirUriFlow(context: Context, widgetId: Int): Flow<Uri?> =
        context.widgetDataStore.data.map { prefs ->
            prefs[vaultDirUriKey(widgetId)]?.let { Uri.parse(it) }
        }

    suspend fun setVaultDirUri(context: Context, widgetId: Int, uri: Uri) {
        context.widgetDataStore.edit { prefs ->
            prefs[vaultDirUriKey(widgetId)] = uri.toString()
        }
    }

    fun getBackgroundOpacityFlow(context: Context, widgetId: Int): Flow<Int> =
        context.widgetDataStore.data.map { prefs ->
            prefs[backgroundOpacityKey(widgetId)] ?: 80
        }

    suspend fun setBackgroundOpacity(context: Context, widgetId: Int, opacity: Int) {
        context.widgetDataStore.edit { prefs ->
            prefs[backgroundOpacityKey(widgetId)] = opacity
        }
    }

    fun getVaultName(context: Context, vaultDirUri: Uri): String? {
        return DocumentFile.fromTreeUri(context, vaultDirUri)?.name
    }

    /**
     * null = キー未存在（このウィジェットでは未設定。フォールバックで全カレンダー表示）。
     * 空 set = ユーザーが明示的に 0 件を選択した状態（イベントは何も表示されない）。
     */
    fun getSelectedCalendarIdsFlow(context: Context, widgetId: Int): Flow<Set<Long>?> =
        context.widgetDataStore.data.map { prefs ->
            prefs[selectedCalendarIdsKey(widgetId)]?.mapNotNull { it.toLongOrNull() }?.toSet()
        }

    suspend fun setSelectedCalendarIds(context: Context, widgetId: Int, ids: Set<Long>) {
        context.widgetDataStore.edit { prefs ->
            prefs[selectedCalendarIdsKey(widgetId)] = ids.map { it.toString() }.toSet()
        }
    }

    suspend fun deleteWidgetPrefs(context: Context, widgetId: Int) {
        context.widgetDataStore.edit { prefs ->
            prefs.remove(journalDirUriKey(widgetId))
            prefs.remove(filenameFormatKey(widgetId))
            prefs.remove(vaultDirUriKey(widgetId))
            prefs.remove(backgroundOpacityKey(widgetId))
            prefs.remove(selectedCalendarIdsKey(widgetId))
        }
    }
}
