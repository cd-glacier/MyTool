package cdglacier.mytool.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cdglacier.mytool.data.repository.LocationRecordRepository
import cdglacier.mytool.data.repository.TrackingStateRepository
import com.google.android.gms.location.LocationResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

        val appContext = context.applicationContext
        val deps = EntryPointAccessors.fromApplication(appContext, Deps::class.java)
        val pending = goAsync()
        scope.launch {
            try {
                handleLocationUpdate(
                    context = appContext,
                    location = location,
                    locationRecordRepository = deps.locationRecordRepository(),
                    trackingStateRepository = deps.trackingStateRepository(),
                    trackingManager = deps.trackingManager(),
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOCATION_UPDATE = "cdglacier.mytool.ACTION_LOCATION_UPDATE"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
