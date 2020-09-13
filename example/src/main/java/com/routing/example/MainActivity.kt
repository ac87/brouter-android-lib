package com.routing.example

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import btools.router.OsmTrack
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Polyline
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.routing.brouter.BRouter
import com.routing.brouter.BundledProfile
import com.routing.brouter.RoutingParams
import com.routing.brouter.Util
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private var polylines: ArrayList<Polyline> = ArrayList()
    private lateinit var mapboxMap: MapboxMap
    private var mapView: MapView? = null

    private lateinit var rootDirectory: File

    private var mapLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootDirectory = getExternalFilesDir(null)!!
        BRouter.initialise(this, rootDirectory)
        val segmentDir = File(BRouter.segmentsFolderPath(rootDirectory))
        if (!segmentDir.exists()) {
            segmentDir.mkdirs()
            Util.copyAssetFile(this, "W5_N50.rd5", "$segmentDir/W5_N50.rd5")
        }

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        setContentView(R.layout.activity_main)
        buttonMultiple.setOnClickListener {

            // NOTE should do this in a separate thread

            // get first route
            var params = RoutingParams.Builder(rootDirectory)
                    .profile(BundledProfile.CAR_FAST)
                    .from(54.273684, -3.216248)
                    .to(54.327425, -2.739716)
                    .build()

            var result = BRouter.generateRoute(params)
            if (result.success) {

                val track1 = result.track!!

                // get first alternate route
                params = RoutingParams.Builder(rootDirectory)
                        .profile(BundledProfile.CAR_FAST)
                        .from(54.273684, -3.216248)
                        .to(54.327425, -2.739716)
                        .alternateIndex(1)
                        .build()

                result = BRouter.generateRoute(params)
                if (result.success) {
                    val track2 = result.track!!
                    showTrack(track1, true, "#3bb2d0")
                    showTrack(track2, false, "#ff0000")
                } else
                    Toast.makeText(this, "Failed to create route 1 - ${result.exception!!.message}", Toast.LENGTH_SHORT).show()
            } else
                Toast.makeText(this, "Failed to create route 0 - ${result.exception!!.message}", Toast.LENGTH_SHORT).show()
        }

        buttonSingle.setOnClickListener {
            // NOTE should do this in a separate thread

            val params = RoutingParams.Builder(rootDirectory)
                    .profile(BundledProfile.BIKE_TREKKING)
                    .from(54.543592, -2.950076)
                    .addVia(54.530371, -3.004975)
                    .to(54.542671, -2.966995)
                    .build()

            val result = BRouter.generateRoute(params)
            if (result.success)
                showTrack(result.track!!, true, "#3bb2d0")
            else
                Toast.makeText(this, "Failed to create route - ${result.exception!!.message}", Toast.LENGTH_SHORT).show()
        }

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                // Map is set up and the style has loaded. Now you can add data or make other map adjustments
                mapLoaded = true
            }
        }
    }

    private fun showTrack(track: OsmTrack, removeLast: Boolean, color: String) {

        if (!mapLoaded)
            return

        if (removeLast && polylines.isNotEmpty()) {
            for (polyline in polylines) mapboxMap.removePolyline(polyline)
            polylines.clear()
        }

        val points: ArrayList<LatLng> = ArrayList()

        val collection = FeatureCollection.fromJson(track.formatAsGeoJson())
        if (collection != null) {
            val lineString = collection.features()!![0].geometry() as LineString
            for (coordinate in lineString.coordinates()) {
                points.add(LatLng(coordinate.latitude(), coordinate.longitude()))
            }
        }

        if (points.size > 0) {
            polylines.add(mapboxMap.addPolyline(PolylineOptions()
                    .addAll(points)
                    .color(Color.parseColor(color))
                    .width(10f)))

            val bounds = LatLngBounds.Builder()
                    .include(points[0])
                    .include(points[points.size / 4])
                    .include(points[points.size / 2])
                    .include(points[points.size / 4*3])
                    .include(points[points.size - 1])
                    .build()

            val cameraPosition = mapboxMap.getCameraForLatLngBounds(bounds)!!

            mapboxMap.cameraPosition = CameraPosition.Builder()
                    .target(cameraPosition.target)
                    .zoom(cameraPosition.zoom - 1)
                    .build()
        }
    }
}
