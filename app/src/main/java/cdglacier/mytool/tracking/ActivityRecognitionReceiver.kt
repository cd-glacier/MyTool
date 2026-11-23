package cdglacier.mytool.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cdglacier.mytool.data.repository.TrackingMode
import cdglacier.mytool.data.repository.TrackingStateRepository
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ActivityRecognition の遷移通知を受けてトラッキングモードを切替える。
 * MOVING 系（IN_VEHICLE / ON_BICYCLE / RUNNING / WALKING）の ENTER で MOVING に、
 * STILL の ENTER で STATIONARY に切り替える。
 */
class ActivityRecognitionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun trackingStateRepository(): TrackingStateRepository
        fun trackingManager(): LocationTrackingManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ACTIVITY_TRANSITION) return
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return

        val appContext = context.applicationContext
        val deps = EntryPointAccessors.fromApplication(appContext, Deps::class.java)
        val pending = goAsync()
        scope.launch {
            try {
                if (!deps.trackingStateRepository().trackingEnabled.first()) return@launch
                val latestTarget = result.transitionEvents
                    .lastOrNull { it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER }
                    ?: return@launch
                when (latestTarget.activityType) {
                    DetectedActivity.IN_VEHICLE,
                    DetectedActivity.ON_BICYCLE,
                    DetectedActivity.RUNNING,
                    DetectedActivity.WALKING -> deps.trackingManager().switchMode(TrackingMode.MOVING)
                    DetectedActivity.STILL -> deps.trackingManager().switchMode(TrackingMode.STATIONARY)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ACTIVITY_TRANSITION = "cdglacier.mytool.ACTION_ACTIVITY_TRANSITION"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val MOVING_ACTIVITIES = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING,
        )
    }
}
