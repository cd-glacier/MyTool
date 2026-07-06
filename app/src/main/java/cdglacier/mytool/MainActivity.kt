package cdglacier.mytool

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import cdglacier.mytool.navigation.CopyObsidianJournalRoute as CopyObsidianJournalNav
import cdglacier.mytool.navigation.HabitTrackingRoute as HabitTrackingNav
import cdglacier.mytool.navigation.HomeRoute as HomeNav
import cdglacier.mytool.navigation.HouseholdRoute as HouseholdNav
import cdglacier.mytool.navigation.MoneyChartRoute as MoneyChartNav
import cdglacier.mytool.navigation.MoneyRoute as MoneyNav
import cdglacier.mytool.navigation.PositionTrackingRoute as PositionTrackingNav
import cdglacier.mytool.navigation.RecipeRoute as RecipeNav
import cdglacier.mytool.navigation.SettingsRoute as SettingsNav
import cdglacier.mytool.ui.screen.copyjournal.CopyObsidianJournalRoute
import cdglacier.mytool.ui.screen.habit.HabitTrackingRoute
import cdglacier.mytool.ui.screen.home.HomeRoute
import cdglacier.mytool.ui.screen.household.HouseholdRoute
import cdglacier.mytool.ui.screen.money.MoneyRoute
import cdglacier.mytool.ui.screen.money.chart.MoneyChartRoute
import cdglacier.mytool.ui.screen.positiontracking.PositionTrackingRoute
import cdglacier.mytool.ui.screen.recipe.RecipeRoute
import cdglacier.mytool.ui.screen.settings.SettingsRoute
import cdglacier.mytool.ui.theme.MyToolTheme
import cdglacier.mytool.widget.CalendarWidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingSharedUrl by mutableStateOf<String?>(null)

    override fun onResume() {
        super.onResume()
        CalendarWidgetUpdateWorker.runOnce(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingSharedUrl = extractSharedUrl(intent)
        setContent {
            MyToolTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val backStack = rememberNavBackStack(HomeNav)
                    LaunchedEffect(pendingSharedUrl) {
                        val url = pendingSharedUrl ?: return@LaunchedEffect
                        backStack.add(RecipeNav(prefilledUrl = url))
                        pendingSharedUrl = null
                    }
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        transitionSpec = {
                            slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                        },
                        popTransitionSpec = {
                            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { it })
                        },
                        predictivePopTransitionSpec = {
                            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { it })
                        },
                        entryProvider = entryProvider {
                            entry<HomeNav> {
                                HomeRoute(
                                    onNavigateToCopyObsidianJournal = { backStack.add(CopyObsidianJournalNav) },
                                    onNavigateToHabitTracking = { backStack.add(HabitTrackingNav) },
                                    onNavigateToPositionTracking = { backStack.add(PositionTrackingNav) },
                                    onNavigateToMoney = { backStack.add(MoneyNav) },
                                    onNavigateToRecipe = { backStack.add(RecipeNav()) },
                                    onNavigateToHousehold = { backStack.add(HouseholdNav) },
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
                            entry<HouseholdNav> {
                                HouseholdRoute(onBack = { backStack.removeLastOrNull() })
                            }
                            entry<RecipeNav> { route ->
                                RecipeRoute(
                                    onBack = { backStack.removeLastOrNull() },
                                    prefilledUrl = route.prefilledUrl,
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedUrl(intent)?.let { pendingSharedUrl = it }
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return Regex("""https?://\S+""").find(text)?.value
    }
}
