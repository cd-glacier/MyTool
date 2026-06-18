package cdglacier.mytool.ui.component

import android.graphics.Color as AndroidColor
import android.preference.PreferenceManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import cdglacier.mytool.data.db.LocationRecordEntity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmMapView(
    records: List<LocationRecordEntity>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context),
        )
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
            }
        },
        update = { map ->
            map.overlays.clear()
            if (records.isNotEmpty()) {
                val points = records.map { GeoPoint(it.latitude, it.longitude) }
                map.overlays.add(Polyline().apply {
                    setPoints(points)
                    outlinePaint.color = AndroidColor.parseColor("#4DD0E1")
                    outlinePaint.strokeWidth = 6f
                })
                records.forEach { rec ->
                    map.overlays.add(Marker(map).apply {
                        position = GeoPoint(rec.latitude, rec.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "acc=${rec.accuracy.toInt()}m bat=${rec.batteryLevel}% stay=${rec.sameLocationCount}"
                    })
                }
                map.controller.setCenter(points.last())
            }
            map.invalidate()
        },
    )
}
