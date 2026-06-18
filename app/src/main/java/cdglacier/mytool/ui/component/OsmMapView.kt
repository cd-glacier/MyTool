package cdglacier.mytool.ui.component

import android.graphics.Color as AndroidColor
import android.preference.PreferenceManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cdglacier.mytool.ui.screen.positiontracking.LocationPointUiModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmMapView(
    points: List<LocationPointUiModel>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context),
        )
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.clear()
            if (points.isNotEmpty()) {
                val geo = points.map { GeoPoint(it.latitude, it.longitude) }
                map.overlays.add(Polyline().apply {
                    setPoints(geo)
                    outlinePaint.color = AndroidColor.parseColor("#4DD0E1")
                    outlinePaint.strokeWidth = 6f
                })
                points.forEach { p ->
                    map.overlays.add(Marker(map).apply {
                        position = GeoPoint(p.latitude, p.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "acc=${p.accuracy.toInt()}m bat=${p.batteryLevel}% stay=${p.sameLocationCount}"
                    })
                }
                map.controller.setCenter(geo.last())
            }
            map.invalidate()
        },
    )
}
