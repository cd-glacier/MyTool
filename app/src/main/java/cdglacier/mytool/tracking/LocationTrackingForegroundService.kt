package cdglacier.mytool.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import cdglacier.mytool.MainActivity
import cdglacier.mytool.R
import cdglacier.mytool.data.repository.LocationRecordRepository
import cdglacier.mytool.data.repository.TrackingStateRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 高精度（MOVING）モード中のみ常駐する ForegroundService。
 * FusedLocationProviderClient へ LocationCallback で直接登録する。
 */
@AndroidEntryPoint
class LocationTrackingForegroundService : Service() {

    @Inject lateinit var locationRecordRepository: LocationRecordRepository
    @Inject lateinit var trackingStateRepository: TrackingStateRepository
    @Inject lateinit var trackingManager: LocationTrackingManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            scope.launch {
                handleLocationUpdate(
                    context = applicationContext,
                    location = location,
                    locationRecordRepository = locationRecordRepository,
                    trackingStateRepository = trackingStateRepository,
                    trackingManager = trackingManager,
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannel(this)
        startForeground(
            NOTIFICATION_ID,
            buildNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        requestUpdates()
        return START_STICKY
    }

    @Suppress("MissingPermission")
    private fun requestUpdates() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LocationTrackingManager.MOVING_INTERVAL_MS,
        )
            .setMinUpdateIntervalMillis(LocationTrackingManager.MOVING_INTERVAL_MS)
            .build()
        client.requestLocationUpdates(request, callback, mainLooper)
    }

    override fun onDestroy() {
        client.removeLocationUpdates(callback)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "location_tracking"
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingForegroundService::class.java))
        }

        fun ensureNotificationChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.location_tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.location_tracking_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        private fun buildNotification(context: Context): Notification {
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_POSITION_TRACKING
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.location_tracking_notification_title))
                .setContentText(context.getString(R.string.location_tracking_notification_text))
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        const val ACTION_OPEN_POSITION_TRACKING = "cdglacier.mytool.ACTION_OPEN_POSITION_TRACKING"
    }
}
