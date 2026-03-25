package cdglacier.mytool.data.repository

import android.content.Context
import android.net.Uri
import cdglacier.mytool.obsidianDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

interface ObsidianRepository {
    val vaultUri: Flow<Uri?>
    val journalDirUri: Flow<Uri?>
    val filenameFormat: Flow<String>

    suspend fun setVaultUri(uri: Uri)
    suspend fun setJournalDirUri(uri: Uri)
    suspend fun setFilenameFormat(format: String)
}

@Singleton
class ObsidianRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ObsidianRepository {

    private val VAULT_URI_KEY = stringPreferencesKey("vault_uri")
    private val JOURNAL_DIR_URI_KEY = stringPreferencesKey("journal_dir_uri")
    private val JOURNAL_FILENAME_FORMAT_KEY = stringPreferencesKey("journal_filename_format")

    override val vaultUri: Flow<Uri?> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[VAULT_URI_KEY]?.let { Uri.parse(it) }
        }

    override val journalDirUri: Flow<Uri?> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[JOURNAL_DIR_URI_KEY]?.let { Uri.parse(it) }
        }

    override val filenameFormat: Flow<String> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[JOURNAL_FILENAME_FORMAT_KEY] ?: DEFAULT_FILENAME_FORMAT
        }

    override suspend fun setVaultUri(uri: Uri) {
        context.obsidianDataStore.edit { prefs ->
            prefs[VAULT_URI_KEY] = uri.toString()
        }
    }

    override suspend fun setJournalDirUri(uri: Uri) {
        context.obsidianDataStore.edit { prefs ->
            prefs[JOURNAL_DIR_URI_KEY] = uri.toString()
        }
    }

    override suspend fun setFilenameFormat(format: String) {
        context.obsidianDataStore.edit { prefs ->
            prefs[JOURNAL_FILENAME_FORMAT_KEY] = format
        }
    }

    companion object {
        const val DEFAULT_FILENAME_FORMAT = "yyyy-MM-dd"
    }
}
