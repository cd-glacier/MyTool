package cdglacier.mytool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import cdglacier.mytool.navigation.CopyObsidianJournalRoute
import cdglacier.mytool.navigation.HomeRoute
import cdglacier.mytool.navigation.SettingsRoute
import cdglacier.mytool.ui.screen.copyjournal.CopyObsidianJournalScreen
import cdglacier.mytool.ui.screen.home.HomeScreen
import cdglacier.mytool.ui.screen.settings.SettingsScreen
import cdglacier.mytool.ui.theme.MyToolTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                                    onNavigateToCopyObsidianJournal = { backStack.add(CopyObsidianJournalRoute) },
                                    onNavigateToSettings = { backStack.add(SettingsRoute) },
                                )
                            }
                            entry<CopyObsidianJournalRoute> {
                                CopyObsidianJournalScreen(onBack = { backStack.removeLastOrNull() })
                            }
                            entry<SettingsRoute> {
                                SettingsScreen(onBack = { backStack.removeLastOrNull() })
                            }
                        }
                    )
                }
            }
        }
    }
}
