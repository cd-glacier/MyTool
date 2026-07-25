package cdglacier.mytool.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface HealthRepository {
    val isAvailable: Boolean
    val permissionsGranted: StateFlow<Boolean>
    val requiredPermissions: Set<String>
    suspend fun refreshPermissions()
    suspend fun getStepsBetween(start: Instant, end: Instant): Long?
    suspend fun getSleepDurationBetween(start: Instant, end: Instant): Duration?
}

@Singleton
class HealthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HealthRepository {

    override val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    private val _granted = MutableStateFlow(false)
    override val permissionsGranted: StateFlow<Boolean> = _granted.asStateFlow()

    private val client: HealthConnectClient?
        get() = if (isAvailable) HealthConnectClient.getOrCreate(context) else null

    override suspend fun refreshPermissions() {
        val c = client ?: run {
            _granted.value = false
            return
        }
        val granted = c.permissionController.getGrantedPermissions()
        _granted.value = granted.containsAll(requiredPermissions)
    }

    override suspend fun getStepsBetween(start: Instant, end: Instant): Long? = runCatching {
        val c = client ?: return null
        val result: AggregationResult = c.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            )
        )
        result[StepsRecord.COUNT_TOTAL]
    }.getOrNull()

    override suspend fun getSleepDurationBetween(start: Instant, end: Instant): Duration? = runCatching {
        val c = client ?: return null
        val result = c.aggregate(
            AggregateRequest(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            )
        )
        result[SleepSessionRecord.SLEEP_DURATION_TOTAL]
    }.getOrNull()
}

object HealthPermissions {
    fun createRequestPermissionResultContract() =
        PermissionController.createRequestPermissionResultContract()
}
