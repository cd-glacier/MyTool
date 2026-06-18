package cdglacier.mytool.tracking

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import cdglacier.mytool.data.repository.TrackingMode
import cdglacier.mytool.data.repository.TrackingStateRepository
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * バックグラウンド位置情報トラッキングの開始/停止/精度切替を担当する。
 * 内部的に FusedLocationProviderClient へ PendingIntent ベースでリクエスト登録する。
 */
@Singleton
class LocationTrackingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingStateRepository: TrackingStateRepository,
) {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    suspend fun start(mode: TrackingMode = TrackingMode.STATIONARY) {
        if (!hasLocationPermission()) return
        trackingStateRepository.setTrackingEnabled(true)
        trackingStateRepository.setMode(mode)
        trackingStateRepository.setLastSignificantMoveAt(System.currentTimeMillis())
        requestUpdates(mode)
    }

    suspend fun stop() {
        trackingStateRepository.setTrackingEnabled(false)
        client.removeLocationUpdates(buildPendingIntent())
    }

    /** Receiver からモード切替を行う。前回と同じなら何もしない。 */
    suspend fun switchMode(newMode: TrackingMode) {
        if (trackingStateRepository.getModeNow() == newMode) return
        trackingStateRepository.setMode(newMode)
        if (trackingStateRepository.isTrackingEnabledNow()) {
            requestUpdates(newMode)
        }
    }

    @Suppress("MissingPermission")
    private fun requestUpdates(mode: TrackingMode) {
        if (!hasLocationPermission()) return
        val (priority, interval) = when (mode) {
            TrackingMode.STATIONARY -> Priority.PRIORITY_BALANCED_POWER_ACCURACY to STATIONARY_INTERVAL_MS
            TrackingMode.MOVING -> Priority.PRIORITY_HIGH_ACCURACY to MOVING_INTERVAL_MS
        }
        val request = LocationRequest.Builder(priority, interval)
            .setMinUpdateIntervalMillis(interval)
            .build()
        client.requestLocationUpdates(request, buildPendingIntent())
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, LocationUpdateReceiver::class.java).apply {
            action = LocationUpdateReceiver.ACTION_LOCATION_UPDATE
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val REQUEST_CODE = 1001
        const val STATIONARY_INTERVAL_MS = 30_000L
        const val MOVING_INTERVAL_MS = 5_000L
        const val MOVE_THRESHOLD_METERS = 100.0
        const val SAME_LOCATION_THRESHOLD_METERS = 10.0
        const val STATIONARY_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
