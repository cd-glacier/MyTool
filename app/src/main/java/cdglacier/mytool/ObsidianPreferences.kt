package cdglacier.mytool

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.obsidianDataStore: DataStore<Preferences> by preferencesDataStore(name = "obsidian_prefs")

object ObsidianPreferences {
    private val VAULT_URI_KEY = stringPreferencesKey("vault_uri")
    private val JOURNAL_DIR_URI_KEY = stringPreferencesKey("journal_dir_uri")
    private val JOURNAL_FILENAME_FORMAT_KEY = stringPreferencesKey("journal_filename_format")

    const val DEFAULT_FILENAME_FORMAT = "yyyy-MM-dd"

    fun getVaultUriFlow(context: Context): Flow<Uri?> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[VAULT_URI_KEY]?.let { Uri.parse(it) }
        }

    suspend fun setVaultUri(context: Context, uri: Uri) {
        context.obsidianDataStore.edit { prefs ->
            prefs[VAULT_URI_KEY] = uri.toString()
        }
    }

    fun getJournalDirUriFlow(context: Context): Flow<Uri?> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[JOURNAL_DIR_URI_KEY]?.let { Uri.parse(it) }
        }

    suspend fun setJournalDirUri(context: Context, uri: Uri) {
        context.obsidianDataStore.edit { prefs ->
            prefs[JOURNAL_DIR_URI_KEY] = uri.toString()
        }
    }

    fun getFilenameFormatFlow(context: Context): Flow<String> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[JOURNAL_FILENAME_FORMAT_KEY] ?: DEFAULT_FILENAME_FORMAT
        }

    suspend fun setFilenameFormat(context: Context, format: String) {
        context.obsidianDataStore.edit { prefs ->
            prefs[JOURNAL_FILENAME_FORMAT_KEY] = format
        }
    }
}
