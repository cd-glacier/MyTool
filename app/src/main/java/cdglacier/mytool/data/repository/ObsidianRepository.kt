package cdglacier.mytool.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.obsidianDataStore: DataStore<Preferences> by preferencesDataStore(name = "obsidian_prefs")

interface ObsidianRepository {
    val vaultUri: Flow<Uri?>
    val journalDirUri: Flow<Uri?>
    val filenameFormat: Flow<String>
    val autoCopyEnabled: Flow<Boolean>
    val pagesDir: Flow<String>

    suspend fun setVaultUri(uri: Uri)
    suspend fun setJournalDirUri(uri: Uri)
    suspend fun setFilenameFormat(format: String)
    suspend fun setAutoCopyEnabled(enabled: Boolean)
    suspend fun setPagesDir(dir: String)
}

@Singleton
class ObsidianRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ObsidianRepository {

    private val VAULT_URI_KEY = stringPreferencesKey("vault_uri")
    private val JOURNAL_DIR_URI_KEY = stringPreferencesKey("journal_dir_uri")
    private val JOURNAL_FILENAME_FORMAT_KEY = stringPreferencesKey("journal_filename_format")
    private val JOURNAL_AUTO_COPY_ENABLED_KEY = booleanPreferencesKey("journal_auto_copy_enabled")
    private val PAGES_DIR_KEY = stringPreferencesKey("pages_dir")

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

    override val autoCopyEnabled: Flow<Boolean> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[JOURNAL_AUTO_COPY_ENABLED_KEY] ?: false
        }

    override suspend fun setAutoCopyEnabled(enabled: Boolean) {
        context.obsidianDataStore.edit { prefs ->
            prefs[JOURNAL_AUTO_COPY_ENABLED_KEY] = enabled
        }
    }

    override val pagesDir: Flow<String> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[PAGES_DIR_KEY] ?: DEFAULT_PAGES_DIR
        }

    override suspend fun setPagesDir(dir: String) {
        context.obsidianDataStore.edit { prefs ->
            prefs[PAGES_DIR_KEY] = dir
        }
    }

    companion object {
        const val DEFAULT_FILENAME_FORMAT = "yyyy-MM-dd"
        const val DEFAULT_PAGES_DIR = "pages"
    }
}
