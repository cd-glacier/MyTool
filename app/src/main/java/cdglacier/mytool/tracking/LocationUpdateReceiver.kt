package cdglacier.mytool.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.BatteryManager
import cdglacier.mytool.data.db.LocationRecordEntity
import cdglacier.mytool.data.repository.LocationRecordRepository
import cdglacier.mytool.data.repository.TrackingMode
import cdglacier.mytool.data.repository.TrackingStateRepository
import com.google.android.gms.location.LocationResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LocationUpdateReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun locationRecordRepository(): LocationRecordRepository
        fun trackingStateRepository(): TrackingStateRepository
        fun trackingManager(): LocationTrackingManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOCATION_UPDATE) return
        val result = LocationResult.extractResult(intent) ?: return
        val location = result.lastLocation ?: return

        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val pending = goAsync()
        scope.launch {
            try {
                handleLocation(context.applicationContext, location, deps)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleLocation(
        context: Context,
        location: Location,
        deps: Deps,
    ) {
        val repo = deps.locationRecordRepository()
        val stateRepo = deps.trackingStateRepository()
        val manager = deps.trackingManager()

        val latest = repo.getLatest()
        val now = System.currentTimeMillis()
        val distanceToLatest = latest?.let { distanceMeters(it.latitude, it.longitude, location.latitude, location.longitude) }

        if (latest != null && distanceToLatest != null && distanceToLatest <= LocationTrackingManager.SAME_LOCATION_THRESHOLD_METERS) {
            // 同位置: カウンタ++、timestamp更新、バッテリーは据え置き
            repo.update(
                latest.copy(
                    timestamp = now,
                    sameLocationCount = latest.sameLocationCount + 1,
                )
            )
        } else {
            // 新規地点
            repo.insert(
                LocationRecordEntity(
                    timestamp = now,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    batteryLevel = readBatteryLevel(context),
                    sameLocationCount = 0,
                )
            )
            stateRepo.setLastSignificantMoveAt(now)
        }

        // モード切替判定
        val currentMode = stateRepo.mode.first()
        when (currentMode) {
            TrackingMode.STATIONARY -> {
                if (distanceToLatest != null && distanceToLatest >= LocationTrackingManager.MOVE_THRESHOLD_METERS) {
                    manager.switchMode(TrackingMode.MOVING)
                }
            }
            TrackingMode.MOVING -> {
                val lastMoveAt = stateRepo.getLastSignificantMoveAt()
                if (now - lastMoveAt >= LocationTrackingManager.STATIONARY_TIMEOUT_MS) {
                    manager.switchMode(TrackingMode.STATIONARY)
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

    companion object {
        const val ACTION_LOCATION_UPDATE = "cdglacier.mytool.ACTION_LOCATION_UPDATE"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
