package cdglacier.mytool.ui.screen.healthconnect

import cdglacier.mytool.domain.model.HealthMetric

internal fun formatMetricValue(value: Double, metric: HealthMetric?): String =
    if (metric == HealthMetric.SLEEP_MINUTES) {
        formatSleepMinutes(value.toLong())
    } else {
        "${formatHealthValue(value)}${metric?.unit?.let { " $it" }.orEmpty()}"
    }

internal fun formatHealthValue(v: Double): String = when {
    v.isNaN() -> "-"
    v == v.toLong().toDouble() -> v.toLong().toString()
    else -> "%.1f".format(v)
}

internal fun formatSleepMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h} hour ${m} min"
}
