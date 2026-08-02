package cdglacier.mytool.domain.model

import java.time.LocalDate

data class DailyHealth(
    val date: LocalDate,
    val steps: Long? = null,
    val distanceMeters: Double? = null,
    val activeMinutes: Long? = null,
    val activeCalories: Double? = null,
    val totalCalories: Double? = null,
    val heartRateAvg: Long? = null,
    val heartRateMin: Long? = null,
    val heartRateMax: Long? = null,
    val restingHeartRate: Long? = null,
    val sleepMinutes: Long? = null,
    val bloodPressureSystolic: Double? = null,
    val bloodPressureDiastolic: Double? = null,
    val bloodGlucose: Double? = null,
    val spo2: Double? = null,
    val bodyTemperature: Double? = null,
    val respiratoryRate: Double? = null,
) {
    fun hasAny(): Boolean = listOf(
        steps, distanceMeters, activeMinutes, activeCalories, totalCalories,
        heartRateAvg, heartRateMin, heartRateMax, restingHeartRate, sleepMinutes,
        bloodPressureSystolic, bloodPressureDiastolic, bloodGlucose, spo2,
        bodyTemperature, respiratoryRate,
    ).any { it != null }
}

data class HealthBook(
    val days: Map<LocalDate, DailyHealth> = emptyMap(),
) {
    fun dayOrEmpty(date: LocalDate): DailyHealth = days[date] ?: DailyHealth(date)
    fun with(day: DailyHealth): HealthBook = copy(days = days + (day.date to day))
}

enum class HealthMetric(val key: String, val label: String, val unit: String) {
    STEPS("steps", "STEPS", "歩"),
    DISTANCE("distance", "DISTANCE", "km"),
    ACTIVE_MINUTES("active_min", "ACTIVE_TIME", "分"),
    ACTIVE_CALORIES("active_kcal", "ACTIVE_KCAL", "kcal"),
    TOTAL_CALORIES("total_kcal", "TOTAL_KCAL", "kcal"),
    HEART_RATE_AVG("hr_avg", "HR_AVG", "bpm"),
    HEART_RATE_MIN("hr_min", "HR_MIN", "bpm"),
    HEART_RATE_MAX("hr_max", "HR_MAX", "bpm"),
    RESTING_HEART_RATE("resting_hr", "RESTING_HR", "bpm"),
    SLEEP_MINUTES("sleep_min", "SLEEP", "min"),
    BP_SYSTOLIC("bp_sys", "BP_SYS", "mmHg"),
    BP_DIASTOLIC("bp_dia", "BP_DIA", "mmHg"),
    BLOOD_GLUCOSE("glucose", "GLUCOSE", "mmol/L"),
    SPO2("spo2", "SPO2", "%"),
    BODY_TEMPERATURE("body_temp", "BODY_TEMP", "°C"),
    RESPIRATORY_RATE("resp_rate", "RESP_RATE", "rpm");

    fun valueOf(day: DailyHealth): Double? = when (this) {
        STEPS -> day.steps?.toDouble()
        DISTANCE -> day.distanceMeters?.let { it / 1000.0 }
        ACTIVE_MINUTES -> day.activeMinutes?.toDouble()
        ACTIVE_CALORIES -> day.activeCalories
        TOTAL_CALORIES -> day.totalCalories
        HEART_RATE_AVG -> day.heartRateAvg?.toDouble()
        HEART_RATE_MIN -> day.heartRateMin?.toDouble()
        HEART_RATE_MAX -> day.heartRateMax?.toDouble()
        RESTING_HEART_RATE -> day.restingHeartRate?.toDouble()
        SLEEP_MINUTES -> day.sleepMinutes?.toDouble()
        BP_SYSTOLIC -> day.bloodPressureSystolic
        BP_DIASTOLIC -> day.bloodPressureDiastolic
        BLOOD_GLUCOSE -> day.bloodGlucose
        SPO2 -> day.spo2
        BODY_TEMPERATURE -> day.bodyTemperature
        RESPIRATORY_RATE -> day.respiratoryRate
    }

    companion object {
        fun fromKey(key: String): HealthMetric? = values().firstOrNull { it.key == key }
    }
}

enum class HealthCategory(val label: String, val metrics: List<HealthMetric>) {
    ACTIVITY(
        "ACTIVITY",
        listOf(
            HealthMetric.STEPS,
            HealthMetric.DISTANCE,
            HealthMetric.ACTIVE_MINUTES,
            HealthMetric.ACTIVE_CALORIES,
            HealthMetric.TOTAL_CALORIES,
        ),
    ),
    SLEEP("SLEEP", listOf(HealthMetric.SLEEP_MINUTES)),
    HEART(
        "HEART",
        listOf(
            HealthMetric.HEART_RATE_AVG,
            HealthMetric.HEART_RATE_MIN,
            HealthMetric.HEART_RATE_MAX,
            HealthMetric.RESTING_HEART_RATE,
        ),
    ),
    VITALS(
        "VITALS",
        listOf(
            HealthMetric.BP_SYSTOLIC,
            HealthMetric.BP_DIASTOLIC,
            HealthMetric.BLOOD_GLUCOSE,
            HealthMetric.SPO2,
            HealthMetric.BODY_TEMPERATURE,
            HealthMetric.RESPIRATORY_RATE,
        ),
    );
}
