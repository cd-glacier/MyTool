package cdglacier.mytool.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data object CopyObsidianJournalRoute : NavKey

@Serializable
data object HabitTrackingRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object PositionTrackingRoute : NavKey

@Serializable
data object MoneyRoute : NavKey

@Serializable
data class MoneyChartRoute(
    val section: String,
    val group: String? = null,
) : NavKey
