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
import cdglacier.mytool.navigation.CopyObsidianJournalRoute as CopyObsidianJournalNav
import cdglacier.mytool.navigation.HabitTrackingRoute as HabitTrackingNav
import cdglacier.mytool.navigation.HomeRoute as HomeNav
import cdglacier.mytool.navigation.MoneyChartRoute as MoneyChartNav
import cdglacier.mytool.navigation.MoneyRoute as MoneyNav
import cdglacier.mytool.navigation.PositionTrackingRoute as PositionTrackingNav
import cdglacier.mytool.navigation.SettingsRoute as SettingsNav
import cdglacier.mytool.ui.screen.copyjournal.CopyObsidianJournalRoute
import cdglacier.mytool.ui.screen.habit.HabitTrackingRoute
import cdglacier.mytool.ui.screen.home.HomeRoute
import cdglacier.mytool.ui.screen.money.MoneyRoute
import cdglacier.mytool.ui.screen.money.chart.MoneyChartRoute
import cdglacier.mytool.ui.screen.positiontracking.PositionTrackingRoute
import cdglacier.mytool.ui.screen.settings.SettingsRoute
import cdglacier.mytool.ui.theme.MyToolTheme
import cdglacier.mytool.widget.CalendarWidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        CalendarWidgetUpdateWorker.runOnce(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyToolTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val backStack = rememberNavBackStack(HomeNav)
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            entry<HomeNav> {
                                HomeRoute(
                                    onNavigateToCopyObsidianJournal = { backStack.add(CopyObsidianJournalNav) },
                                    onNavigateToHabitTracking = { backStack.add(HabitTrackingNav) },
                                    onNavigateToPositionTracking = { backStack.add(PositionTrackingNav) },
                                    onNavigateToMoney = { backStack.add(MoneyNav) },
                                    onNavigateToSettings = { backStack.add(SettingsNav) },
                                )
                            }
                            entry<MoneyNav> {
                                MoneyRoute(
                                    onBack = { backStack.removeLastOrNull() },
                                    onNavigateChart = { section, group ->
                                        backStack.add(MoneyChartNav(section, group))
                                    },
                                )
                            }
                            entry<MoneyChartNav> { route ->
                                MoneyChartRoute(
                                    section = route.section,
                                    group = route.group,
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            }
                            entry<CopyObsidianJournalNav> {
                                CopyObsidianJournalRoute(onBack = { backStack.removeLastOrNull() })
                            }
                            entry<HabitTrackingNav> {
                                HabitTrackingRoute(onBack = { backStack.removeLastOrNull() })
                            }
                            entry<SettingsNav> {
                                SettingsRoute(onBack = { backStack.removeLastOrNull() })
                            }
                            entry<PositionTrackingNav> {
                                PositionTrackingRoute(onBack = { backStack.removeLastOrNull() })
                            }
                        }
                    )
                }
            }
        }
    }
}
