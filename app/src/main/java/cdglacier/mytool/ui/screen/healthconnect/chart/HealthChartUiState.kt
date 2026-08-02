package cdglacier.mytool.ui.screen.healthconnect.chart

import cdglacier.mytool.domain.model.HealthMetric

enum class HealthChartMode { DAY, WEEK }

data class HealthChartPoint(
    val label: String,
    val value: Double?,
)

data class HealthChartUiState(
    val metric: HealthMetric? = null,
    val mode: HealthChartMode = HealthChartMode.DAY,
    val points: List<HealthChartPoint> = emptyList(),
) {
    val title: String get() = metric?.label ?: "CHART"
    val unit: String get() = metric?.unit ?: ""
}
