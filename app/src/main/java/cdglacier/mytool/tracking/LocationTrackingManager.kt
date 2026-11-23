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
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * バックグラウンド位置情報トラッキングの開始/停止/精度切替を担当する。
 *
 * - STATIONARY: PendingIntent + BroadcastReceiver 方式で 60 秒間隔の BALANCED 取得
 * - MOVING: ForegroundService + LocationCallback 方式で 60 秒間隔の HIGH_ACCURACY (GPS) 取得
 *
 * あわせて ActivityRecognition の遷移検知を購読し、距離ベース判定と OR でモード切替を行う。
 */
@Singleton
class LocationTrackingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingStateRepository: TrackingStateRepository,
) {

    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val activityClient by lazy { ActivityRecognition.getClient(context) }

    suspend fun start(mode: TrackingMode = TrackingMode.STATIONARY) {
        if (!hasLocationPermission()) return
        trackingStateRepository.setTrackingEnabled(true)
        trackingStateRepository.setMode(mode)
        trackingStateRepository.setLastSignificantMoveAt(System.currentTimeMillis())
        applyMode(mode)
        registerActivityRecognition()
    }

    suspend fun stop() {
        trackingStateRepository.setTrackingEnabled(false)
        locationClient.removeLocationUpdates(buildLocationPendingIntent())
        LocationTrackingForegroundService.stop(context)
        unregisterActivityRecognition()
    }

    /** Receiver からモード切替を行う。前回と同じなら何もしない。 */
    suspend fun switchMode(newMode: TrackingMode) {
        if (trackingStateRepository.mode.first() == newMode) return
        trackingStateRepository.setMode(newMode)
        if (trackingStateRepository.trackingEnabled.first()) {
            applyMode(newMode)
        }
    }

    @Suppress("MissingPermission")
    private fun applyMode(mode: TrackingMode) {
        if (!hasLocationPermission()) return
        when (mode) {
            TrackingMode.STATIONARY -> {
                LocationTrackingForegroundService.stop(context)
                val request = LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    STATIONARY_INTERVAL_MS,
                )
                    .setMinUpdateIntervalMillis(STATIONARY_INTERVAL_MS)
                    .build()
                locationClient.requestLocationUpdates(request, buildLocationPendingIntent())
            }
            TrackingMode.MOVING -> {
                locationClient.removeLocationUpdates(buildLocationPendingIntent())
                LocationTrackingForegroundService.start(context)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun registerActivityRecognition() {
        if (!hasActivityRecognitionPermission()) return
        val transitions = buildList {
            ActivityRecognitionReceiver.MOVING_ACTIVITIES.forEach { type ->
                add(
                    ActivityTransition.Builder()
                        .setActivityType(type)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
                )
            }
            add(
                ActivityTransition.Builder()
                    .setActivityType(com.google.android.gms.location.DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
        }
        val request = ActivityTransitionRequest(transitions)
        activityClient.requestActivityTransitionUpdates(request, buildActivityPendingIntent())
    }

    private fun unregisterActivityRecognition() {
        activityClient.removeActivityTransitionUpdates(buildActivityPendingIntent())
    }

    private fun buildLocationPendingIntent(): PendingIntent {
        val intent = Intent(context, LocationUpdateReceiver::class.java).apply {
            action = LocationUpdateReceiver.ACTION_LOCATION_UPDATE
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(context, LOCATION_REQUEST_CODE, intent, flags)
    }

    private fun buildActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = ActivityRecognitionReceiver.ACTION_ACTIVITY_TRANSITION
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(context, ACTIVITY_REQUEST_CODE, intent, flags)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasActivityRecognitionPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val LOCATION_REQUEST_CODE = 1001
        private const val ACTIVITY_REQUEST_CODE = 1002
        const val STATIONARY_INTERVAL_MS = 60_000L
        const val MOVING_INTERVAL_MS = 60_000L
        const val MOVE_THRESHOLD_METERS = 100.0
        const val SAME_LOCATION_THRESHOLD_METERS = 10.0
        const val STATIONARY_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
