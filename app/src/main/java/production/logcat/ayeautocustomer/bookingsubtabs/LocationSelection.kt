package production.logcat.ayeautocustomer.bookingsubtabs

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import io.paperdb.Paper
import production.logcat.ayeautocustomer.R
import production.logcat.ayeautocustomer.models.allStandDataJSON
import production.logcat.ayeautocustomer.models.locationLandMark
import production.logcat.ayeautocustomer.models.villageName
import java.util.*

class LocationSelection : AppCompatActivity(), OnMapReadyCallback, MapboxMap.OnMapClickListener  {

    private lateinit var map: MapboxMap
    private lateinit var villageTextView: TextView
    private lateinit var landMarkTextView: EditText
    private lateinit var mapView: MapView

    private var destinationMarker: Marker?=null
    private var position: LatLng?=null
    private var cityName: String?=null
    private var intentData: Intent?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        Mapbox.getInstance(this, resources.getString(R.string.MAP_BOX_API_KEY))
        super.onCreate(savedInstanceState)
        Paper.init(this)
        setContentView(R.layout.activity_location_selection)
        mapView = findViewById(R.id.mapboxMap)
        villageTextView = findViewById(R.id.panchayat)
        landMarkTextView = findViewById(R.id.subLandMark)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        findViewById<Button>(R.id.loc_confirm).setOnClickListener {
            if(position!=null&&cityName!!.isNotEmpty()&&villageTextView.text.isNotEmpty()&&landMarkTextView.text.isNotEmpty()&&cityName!!.compareTo("null")!=0){
                locationLandMark =landMarkTextView.text.toString()
                villageName =villageTextView.text.toString()
                allStandDataJSON =null
                Paper.book().write("cityName", cityName)
                Paper.book().write("villageName", villageName)
                Paper.book().write("locationLandMark", locationLandMark)
                Paper.book().write("latitude", (position!!.latitude).toString())
                Paper.book().write("longitude", (position!!.longitude).toString())
                finish()
            }
            else{
                Toast.makeText(baseContext, "invalid data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        map.addOnMapClickListener(this)
        intentData=intent
        position = LatLng(
            intentData!!.getStringExtra("latitude")!!.toDouble(), intentData!!.getStringExtra(
                "longitude"
            )!!.toDouble()
        )
        getCompleteAddressString()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position!!, 15.0))
        destinationMarker = map.addMarker(MarkerOptions().position(position))
    }

    @SuppressLint("SetTextI18n")
    private fun getCompleteAddressString(){
        val geoCoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? = geoCoder.getFromLocation(
                position!!.latitude,
                position!!.longitude,
                1
            )
            if (addresses != null) {
                val zip = addresses[0].postalCode
                val road = addresses[0].thoroughfare
                val panchayath = addresses[0].locality
                val gilla =addresses[0].subAdminArea
                val state = addresses[0].adminArea
                villageTextView.text=panchayath
                cityName=gilla
                landMarkTextView.setText("$zip $road $panchayath $gilla $state")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        map.setStyle(Style.MAPBOX_STREETS) {
            enableLocation()
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        if(destinationMarker!=null){
            map.removeMarker(destinationMarker!!)
        }
        position= LatLng(point.latitude, point.longitude)
        getCompleteAddressString()
        destinationMarker = map.addMarker(MarkerOptions().position(point))
        return true
    }
}