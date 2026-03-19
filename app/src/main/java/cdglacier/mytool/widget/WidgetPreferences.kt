package cdglacier.mytool.widget

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WidgetPreferences @Inject constructor(
    @Named("widget") private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_FILENAME_FORMAT = "yyyy-MM-dd"

        fun journalDirUriKey(id: Int) = stringPreferencesKey("widget_${id}_journal_dir_uri")
        fun filenameFormatKey(id: Int) = stringPreferencesKey("widget_${id}_filename_format")
        fun vaultDirUriKey(id: Int) = stringPreferencesKey("widget_${id}_vault_dir_uri")
        fun backgroundOpacityKey(id: Int) = intPreferencesKey("widget_${id}_background_opacity")
    }

    fun getJournalDirUriFlow(widgetId: Int): Flow<Uri?> =
        dataStore.data.map { prefs ->
            prefs[journalDirUriKey(widgetId)]?.let { Uri.parse(it) }
        }

    suspend fun setJournalDirUri(widgetId: Int, uri: Uri) {
        dataStore.edit { prefs ->
            prefs[journalDirUriKey(widgetId)] = uri.toString()
        }
    }

    fun getFilenameFormatFlow(widgetId: Int): Flow<String> =
        dataStore.data.map { prefs ->
            prefs[filenameFormatKey(widgetId)] ?: DEFAULT_FILENAME_FORMAT
        }

    suspend fun setFilenameFormat(widgetId: Int, format: String) {
        dataStore.edit { prefs ->
            prefs[filenameFormatKey(widgetId)] = format
        }
    }

    fun getVaultDirUriFlow(widgetId: Int): Flow<Uri?> =
        dataStore.data.map { prefs ->
            prefs[vaultDirUriKey(widgetId)]?.let { Uri.parse(it) }
        }

    suspend fun setVaultDirUri(widgetId: Int, uri: Uri) {
        dataStore.edit { prefs ->
            prefs[vaultDirUriKey(widgetId)] = uri.toString()
        }
    }

    fun getBackgroundOpacityFlow(widgetId: Int): Flow<Int> =
        dataStore.data.map { prefs ->
            prefs[backgroundOpacityKey(widgetId)] ?: 80
        }

    suspend fun setBackgroundOpacity(widgetId: Int, opacity: Int) {
        dataStore.edit { prefs ->
            prefs[backgroundOpacityKey(widgetId)] = opacity
        }
    }

    fun getVaultName(vaultDirUri: Uri): String? {
        return DocumentFile.fromTreeUri(context, vaultDirUri)?.name
    }

    suspend fun deleteWidgetPrefs(widgetId: Int) {
        dataStore.edit { prefs ->
            prefs.remove(journalDirUriKey(widgetId))
            prefs.remove(filenameFormatKey(widgetId))
            prefs.remove(vaultDirUriKey(widgetId))
            prefs.remove(backgroundOpacityKey(widgetId))
        }
    }
}
