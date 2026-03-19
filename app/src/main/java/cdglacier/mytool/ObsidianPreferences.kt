package cdglacier.mytool

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ObsidianPreferences @Inject constructor(
    @Named("obsidian") private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val DEFAULT_FILENAME_FORMAT = "yyyy-MM-dd"

        private val VAULT_URI_KEY = stringPreferencesKey("vault_uri")
        private val JOURNAL_DIR_URI_KEY = stringPreferencesKey("journal_dir_uri")
        private val JOURNAL_FILENAME_FORMAT_KEY = stringPreferencesKey("journal_filename_format")
    }

    fun getVaultUriFlow(): Flow<Uri?> =
        dataStore.data.map { prefs ->
            prefs[VAULT_URI_KEY]?.let { Uri.parse(it) }
        }

    suspend fun setVaultUri(uri: Uri) {
        dataStore.edit { prefs ->
            prefs[VAULT_URI_KEY] = uri.toString()
        }
    }

    fun getJournalDirUriFlow(): Flow<Uri?> =
        dataStore.data.map { prefs ->
            prefs[JOURNAL_DIR_URI_KEY]?.let { Uri.parse(it) }
        }

    suspend fun setJournalDirUri(uri: Uri) {
        dataStore.edit { prefs ->
            prefs[JOURNAL_DIR_URI_KEY] = uri.toString()
        }
    }

    fun getFilenameFormatFlow(): Flow<String> =
        dataStore.data.map { prefs ->
            prefs[JOURNAL_FILENAME_FORMAT_KEY] ?: DEFAULT_FILENAME_FORMAT
        }

    suspend fun setFilenameFormat(format: String) {
        dataStore.edit { prefs ->
            prefs[JOURNAL_FILENAME_FORMAT_KEY] = format
        }
    }
}
