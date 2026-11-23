package cdglacier.mytool.tracking

import android.content.Context
import android.location.Location
import android.os.BatteryManager
import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.LocationRecordRepository
import cdglacier.mytool.data.repository.TrackingMode
import cdglacier.mytool.data.repository.TrackingStateRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 位置更新受信時の共通処理（DB 保存 + 距離ベースのモード切替判定）。
 * BroadcastReceiver と ForegroundService から共用する。
 */
suspend fun handleLocationUpdate(
    context: Context,
    location: Location,
    locationRecordRepository: LocationRecordRepository,
    trackingStateRepository: TrackingStateRepository,
    trackingManager: LocationTrackingManager,
) {
    val now = System.currentTimeMillis()
    val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
    val latest = locationRecordRepository.getLatestOfDate(today)
    val distanceToLatest = latest?.let {
        distanceMeters(it.latitude, it.longitude, location.latitude, location.longitude)
    }

    if (latest != null && distanceToLatest != null &&
        distanceToLatest <= LocationTrackingManager.SAME_LOCATION_THRESHOLD_METERS
    ) {
        locationRecordRepository.update(
            latest.copy(
                timestamp = now,
                sameLocationCount = latest.sameLocationCount + 1,
            )
        )
    } else {
        locationRecordRepository.insert(
            LocationRecordEntity(
                timestamp = now,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                batteryLevel = readBatteryLevel(context),
                sameLocationCount = 0,
            )
        )
        trackingStateRepository.setLastSignificantMoveAt(now)
    }

    when (trackingStateRepository.mode.first()) {
        TrackingMode.STATIONARY -> {
            if (distanceToLatest != null &&
                distanceToLatest >= LocationTrackingManager.MOVE_THRESHOLD_METERS
            ) {
                trackingManager.switchMode(TrackingMode.MOVING)
            }
        }
        TrackingMode.MOVING -> {
            val lastMoveAt = trackingStateRepository.getLastSignificantMoveAt()
            if (now - lastMoveAt >= LocationTrackingManager.STATIONARY_TIMEOUT_MS) {
                trackingManager.switchMode(TrackingMode.STATIONARY)
            }
        }
    }
}

private fun readBatteryLevel(context: Context): Int {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val result = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, result)
    return result[0].toDouble()
}
