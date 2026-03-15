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

    fun getVaultUriFlow(context: Context): Flow<Uri?> =
        context.obsidianDataStore.data.map { prefs ->
            prefs[VAULT_URI_KEY]?.let { Uri.parse(it) }
        }

    suspend fun setVaultUri(context: Context, uri: Uri) {
        context.obsidianDataStore.edit { prefs ->
            prefs[VAULT_URI_KEY] = uri.toString()
        }
    }
}
