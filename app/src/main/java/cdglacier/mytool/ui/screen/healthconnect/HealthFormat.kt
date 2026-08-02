package cdglacier.mytool.ui.screen.healthconnect

internal fun formatHealthValue(v: Double): String = when {
    v.isNaN() -> "-"
    v == v.toLong().toDouble() -> v.toLong().toString()
    else -> "%.1f".format(v)
}

internal fun formatSleepMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h}h${m}m"
}
