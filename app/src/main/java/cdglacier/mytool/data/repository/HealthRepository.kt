package cdglacier.mytool.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import cdglacier.mytool.domain.model.DailyHealth
import cdglacier.mytool.domain.model.SleepDay
import cdglacier.mytool.domain.model.SleepSession
import cdglacier.mytool.domain.model.SleepStage
import cdglacier.mytool.domain.model.SleepStageType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface HealthRepository {
    val isAvailable: Boolean
    val permissionsGranted: StateFlow<Boolean>
    val requiredPermissions: Set<String>
    suspend fun refreshPermissions()
    suspend fun readDay(date: LocalDate): DailyHealth
    suspend fun readSleepDay(date: LocalDate): SleepDay
}

@Singleton
class HealthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HealthRepository {

    override val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
    )

    private val _granted = MutableStateFlow(false)
    override val permissionsGranted: StateFlow<Boolean> = _granted.asStateFlow()

    private val client: HealthConnectClient?
        get() = if (isAvailable) HealthConnectClient.getOrCreate(context) else null

    override suspend fun refreshPermissions() {
        val c = client ?: run { _granted.value = false; return }
        val granted = c.permissionController.getGrantedPermissions()
        _granted.value = granted.containsAll(requiredPermissions)
    }

    override suspend fun readDay(date: LocalDate): DailyHealth {
        val c = client ?: return DailyHealth(date)
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val range = TimeRangeFilter.between(start, end)

        val steps = aggregateLong(c, StepsRecord.COUNT_TOTAL, range)
        val distance = aggregateDouble(c, DistanceRecord.DISTANCE_TOTAL, range) { it.inMeters }
        val activeKcal = aggregateDouble(c, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL, range) { it.inKilocalories }
        val totalKcal = aggregateDouble(c, TotalCaloriesBurnedRecord.ENERGY_TOTAL, range) { it.inKilocalories }
        val exerciseSec = aggregateLong(c, ExerciseSessionRecord.EXERCISE_DURATION_TOTAL, range) { it.seconds }
        val sleepMinutes = aggregateLong(c, SleepSessionRecord.SLEEP_DURATION_TOTAL, range) { it.toMinutes() }
        val hrAvg = aggregateLong(c, HeartRateRecord.BPM_AVG, range)
        val hrMin = aggregateLong(c, HeartRateRecord.BPM_MIN, range)
        val hrMax = aggregateLong(c, HeartRateRecord.BPM_MAX, range)
        val restingHr = aggregateLong(c, RestingHeartRateRecord.BPM_AVG, range)

        val bpSys = readAvgDouble(c, BloodPressureRecord::class, range) { it.systolic.inMillimetersOfMercury }
        val bpDia = readAvgDouble(c, BloodPressureRecord::class, range) { it.diastolic.inMillimetersOfMercury }
        val glucose = readAvgDouble(c, BloodGlucoseRecord::class, range) { it.level.inMillimolesPerLiter }
        val spo2 = readAvgDouble(c, OxygenSaturationRecord::class, range) { it.percentage.value }
        val temp = readAvgDouble(c, BodyTemperatureRecord::class, range) { it.temperature.inCelsius }
        val respRate = readAvgDouble(c, RespiratoryRateRecord::class, range) { it.rate }

        return DailyHealth(
            date = date,
            steps = steps,
            distanceMeters = distance,
            activeMinutes = exerciseSec?.let { it / 60 },
            activeCalories = activeKcal,
            totalCalories = totalKcal,
            heartRateAvg = hrAvg,
            heartRateMin = hrMin,
            heartRateMax = hrMax,
            restingHeartRate = restingHr,
            sleepMinutes = sleepMinutes,
            bloodPressureSystolic = bpSys,
            bloodPressureDiastolic = bpDia,
            bloodGlucose = glucose,
            spo2 = spo2,
            bodyTemperature = temp,
            respiratoryRate = respRate,
        )
    }

    override suspend fun readSleepDay(date: LocalDate): SleepDay {
        val c = client ?: return SleepDay(date)
        val zone = ZoneId.systemDefault()
        // 前日昼〜翌日昼を対象に日跨ぎの就寝セッションを拾い、中央時刻が対象日に属するもののみ採用
        val start = date.minusDays(1).atTime(12, 0).atZone(zone).toInstant()
        val end = date.plusDays(1).atTime(12, 0).atZone(zone).toInstant()
        val range = TimeRangeFilter.between(start, end)

        val records = runCatching {
            c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range)).records
        }.getOrNull().orEmpty()

        val sessions = records
            .filter { record -> record.midpointDate(zone) == date }
            .map { record ->
                val stages = if (record.stages.isNotEmpty()) {
                    record.stages.map { s ->
                        SleepStage(
                            type = s.stage.toSleepStageType(),
                            start = s.startTime,
                            end = s.endTime,
                        )
                    }
                } else {
                    listOf(
                        SleepStage(
                            type = SleepStageType.SLEEPING,
                            start = record.startTime,
                            end = record.endTime,
                        ),
                    )
                }
                SleepSession(
                    start = record.startTime,
                    end = record.endTime,
                    stages = stages.sortedBy { it.start },
                )
            }
            .sortedBy { it.start }

        return SleepDay(date = date, sessions = sessions)
    }

    private fun SleepSessionRecord.midpointDate(zone: ZoneId): LocalDate {
        val mid = startTime.plusMillis((endTime.toEpochMilli() - startTime.toEpochMilli()) / 2)
        return mid.atZone(zone).toLocalDate()
    }

    private fun Int.toSleepStageType(): SleepStageType = when (this) {
        SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStageType.AWAKE
        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> SleepStageType.AWAKE_IN_BED
        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepStageType.OUT_OF_BED
        SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
        SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStageType.DEEP
        SleepSessionRecord.STAGE_TYPE_REM -> SleepStageType.REM
        SleepSessionRecord.STAGE_TYPE_SLEEPING -> SleepStageType.SLEEPING
        else -> SleepStageType.UNKNOWN
    }

    private suspend fun aggregateLong(
        c: HealthConnectClient,
        metric: androidx.health.connect.client.aggregate.AggregateMetric<Long>,
        range: TimeRangeFilter,
    ): Long? = runCatching {
        c.aggregate(AggregateRequest(setOf(metric), range))[metric]
    }.getOrNull()

    @JvmName("aggregateLongDuration")
    private suspend fun aggregateLong(
        c: HealthConnectClient,
        metric: androidx.health.connect.client.aggregate.AggregateMetric<java.time.Duration>,
        range: TimeRangeFilter,
        transform: (java.time.Duration) -> Long,
    ): Long? = runCatching {
        c.aggregate(AggregateRequest(setOf(metric), range))[metric]?.let(transform)
    }.getOrNull()

    private suspend fun <T : Any> aggregateDouble(
        c: HealthConnectClient,
        metric: androidx.health.connect.client.aggregate.AggregateMetric<T>,
        range: TimeRangeFilter,
        transform: (T) -> Double,
    ): Double? = runCatching {
        c.aggregate(AggregateRequest(setOf(metric), range))[metric]?.let(transform)
    }.getOrNull()

    private suspend fun <T : androidx.health.connect.client.records.Record> readAvgDouble(
        c: HealthConnectClient,
        klass: kotlin.reflect.KClass<T>,
        range: TimeRangeFilter,
        selector: (T) -> Double,
    ): Double? = runCatching {
        val records = c.readRecords(ReadRecordsRequest(klass, range)).records
        if (records.isEmpty()) null
        else records.map(selector).average()
    }.getOrNull()
}

object HealthPermissions {
    fun createRequestPermissionResultContract() =
        PermissionController.createRequestPermissionResultContract()
}
