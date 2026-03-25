package cdglacier.mytool

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.screen.CopyObsidianJournalRoute
import cdglacier.mytool.screen.CopyObsidianJournalScreen
import cdglacier.mytool.screen.HomeRoute
import cdglacier.mytool.screen.HomeScreen
import cdglacier.mytool.screen.SettingsRoute
import cdglacier.mytool.screen.SettingsScreen
import cdglacier.mytool.ui.theme.MyToolTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var obsidianRepository: ObsidianRepository

    private var vaultUri by mutableStateOf<Uri?>(null)
    private var journalDirUri by mutableStateOf<Uri?>(null)
    private var filenameFormat by mutableStateOf(ObsidianPreferences.DEFAULT_FILENAME_FORMAT)

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            lifecycleScope.launch {
                obsidianRepository.setVaultUri(uri)
            }
            vaultUri = uri
        }
    }

    private val journalDirPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            lifecycleScope.launch {
                obsidianRepository.setJournalDirUri(uri)
            }
            journalDirUri = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            obsidianRepository.vaultUri.collect { uri -> vaultUri = uri }
        }
        lifecycleScope.launch {
            obsidianRepository.journalDirUri.collect { uri -> journalDirUri = uri }
        }
        lifecycleScope.launch {
            obsidianRepository.filenameFormat.collect { fmt -> filenameFormat = fmt }
        }

        setContent {
            MyToolTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val backStack = rememberNavBackStack(HomeRoute)
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<HomeRoute> {
                                HomeScreen(
                                    onNavigateToCopyObsidianJournal = {
                                        backStack.add(CopyObsidianJournalRoute)
                                    },
                                    onNavigateToSettings = {
                                        backStack.add(SettingsRoute)
                                    }
                                )
                            }
                            entry<CopyObsidianJournalRoute> {
                                CopyObsidianJournalScreen(
                                    journalDirUri = journalDirUri,
                                    filenameFormat = filenameFormat,
                                    onPickJournalDir = { journalDirPickerLauncher.launch(journalDirUri) },
                                    onFilenameFormatChange = { fmt ->
                                        filenameFormat = fmt
                                        lifecycleScope.launch {
                                            obsidianRepository.setFilenameFormat(fmt)
                                        }
                                    },
                                    onBack = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<SettingsRoute> {
                                SettingsScreen(
                                    vaultUri = vaultUri,
                                    onPickFolder = { folderPickerLauncher.launch(vaultUri) },
                                    onBack = { backStack.removeLastOrNull() }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
