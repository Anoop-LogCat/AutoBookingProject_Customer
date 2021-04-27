package production.logcat.ayeautocustomer.bottomnavigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.wang.avi.AVLoadingIndicatorView
import io.paperdb.Paper
import okhttp3.OkHttpClient
import production.logcat.ayeautocustomer.R
import production.logcat.ayeautocustomer.adapters.StandAdapter
import production.logcat.ayeautocustomer.bookingsubtabs.LocationSelection
import production.logcat.ayeautocustomer.models.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BookingWindow : Fragment() {


    private lateinit var villageTextView: TextView
    private lateinit var landMarkTextView: TextView
    private lateinit var standRecycler: RecyclerView
    private lateinit var locationCardView: MaterialCardView
    private lateinit var headingTextView: TextView
    private lateinit var noStandLayout: LinearLayout
    private lateinit var standLoader:AVLoadingIndicatorView


    private val standList=ArrayList<StandModel>()

    private var navigationController: NavController?=null

    private val REQUEST_CHECK_SETTINGS = 267

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_booking_window, container, false)
    }

    override fun onResume() {
        super.onResume()
        Paper.init(requireActivity())
        if(Paper.book().read<String>("cityName")==null||
            Paper.book().read<String>("villageName")==null||
            Paper.book().read<String>("locationLandMark")==null||
            Paper.book().read<String>("latitude")==null||
            Paper.book().read<String>("longitude")==null){
            villageTextView.text= villageName
            landMarkTextView.text= locationLandMark
            noStandLayoutFunc("NO_STAND")
        }
        else{
            villageTextView.text= Paper.book().read("villageName")
            landMarkTextView.text= Paper.book().read("locationLandMark")
            if(allStandDataJSON ==null){
                GetStandInfo().execute(Paper.book().read("cityName"))
            }
            else{
                setData()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationController= Navigation.findNavController(view)
        standRecycler=view.findViewById(R.id.standListRecycler)
        locationCardView=view.findViewById(R.id.location_button)
        villageTextView=view.findViewById(R.id.cityName)
        landMarkTextView=view.findViewById(R.id.landmark)
        headingTextView=view.findViewById(R.id.nearByStand)
        standLoader=view.findViewById(R.id.standLoader)
        noStandLayout=view.findViewById(R.id.noDataDisplayLayout_Stand)

        landMarkTextView.isSelected=true
        locationCardView.setOnClickListener {
            locationProvided=true
            checkPermissionAndLocation()
        }
    }

    private fun noStandLayoutFunc(mode:String){
        when(mode){
            "NO_STAND"->{
                headingTextView.visibility=View.GONE
                standRecycler.visibility=View.GONE
                noStandLayout.visibility=View.VISIBLE
                standLoader.hide()
            }
            "YES_STAND"->{
                headingTextView.visibility=View.VISIBLE
                standRecycler.visibility=View.VISIBLE
                noStandLayout.visibility=View.GONE
                standLoader.hide()
            }
            "LOADING"->{
                headingTextView.visibility=View.GONE
                standRecycler.visibility=View.GONE
                noStandLayout.visibility=View.GONE
                standLoader.show()
            }
            "NOTHING"->{
                headingTextView.visibility=View.GONE
                standRecycler.visibility=View.GONE
                noStandLayout.visibility=View.GONE
                standLoader.hide()
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    private inner class GetStandInfo : AsyncTask<String, Void, String>(){
        override fun onPreExecute() {
            super.onPreExecute()
            noStandLayoutFunc("LOADING")
        }
        override fun doInBackground(vararg params: String?): String {
            return try {
                val url = "https://us-central1-auto-pickup-apps.cloudfunctions.net/CustomerStandView/${params[0]!!}"
                val request: okhttp3.Request = okhttp3.Request.Builder().url(url).build()
                val response= OkHttpClient().newCall(request).execute()
                response.body()?.string().toString()
            }catch (e:Exception){
                "ERROR"
            }
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(result!!.compareTo("ERROR")==0){
                dialogAlertBox(resources.getDrawable(R.drawable.tagdialogbackgroundstyle),requireContext(),"No Connectivity", "Please turn on the internet and press OK", "OK", "", R.drawable.smartphone, okFunc = {
                    GetStandInfo().execute(Paper.book().read("cityName"))
                }, cancelFunc = {}, okVisible = true, cancelVisible = false)
            }
            else{
                allStandDataJSON = result
                setData()
            }
        }
    }

    private fun setData(){
        if(standList.isNotEmpty()){standList.clear()}
        if(allStandDataJSON!!.compareTo("no data")==0){
            noStandLayoutFunc("NO_STAND")
        }
        else{
            noStandLayoutFunc("YES_STAND")
            val allDocMap: java.util.HashMap<*, *>? = Gson().fromJson(allStandDataJSON,HashMap::class.java)
            for(key in allDocMap!!.keys){
                val standObject = Gson().fromJson(Gson().toJson(allDocMap[key]), FireStandModel::class.java)
                if(!standObject.testMode){
                    val distance = getDistanceFromLatLongInKm(standObject.latitude.toDouble(),standObject.longitude.toDouble(),Paper.book().read<String>("latitude").toDouble(),Paper.book().read<String>("longitude").toDouble())
                    standList.add(StandModel(standObject.standName,(distance.toInt()).toString(),standObject.landMark,nextPage = { standNameArg, standLandMarkArg->
                        val action= BookingWindowDirections.actionBookingWindowToDriversWindow2()
                        action.standName=standNameArg
                        action.standLandMark=standLandMarkArg
                        action.currentLatitude=Paper.book().read<String>("latitude")
                        action.currentLongitude=Paper.book().read<String>("longitude")
                        navigationController!!.navigate(action)
                    }))
                }
            }
            val sortedStandList = standList.sortedBy { it.standDistance }
            val adapter = StandAdapter(sortedStandList)
            standRecycler.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            standRecycler.adapter = adapter
        }
    }

    private fun getDistanceFromLatLongInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(lon1 - lon2)
        val a = (sin(latDistance / 2) * sin(latDistance / 2) + (cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(lngDistance / 2) * sin(lngDistance / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (6371 * c)
    }

    private fun checkPermissionAndLocation(){
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CHECK_SETTINGS)
            }
            else{
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            val builder = LocationSettingsRequest.Builder().addLocationRequest(LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
            builder.addLocationRequest(LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
            builder.setAlwaysShow(true)
            val mSettingsClient: SettingsClient = LocationServices.getSettingsClient(requireActivity())
            val mLocationSettingsRequest: LocationSettingsRequest = builder.build()
            val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest).addOnSuccessListener {
                val reqSetting = LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    interval = 10000
                    fastestInterval =10000
                    smallestDisplacement = 1.0f
                }
                val locationUpdates = object : LocationCallback() {
                    override fun onLocationResult(lr: LocationResult) {
                        if(locationProvided){
                            val intentData = Intent(requireActivity(), LocationSelection::class.java)
                            intentData.putExtra("latitude", (lr.lastLocation.latitude).toString())
                            intentData.putExtra("longitude", (lr.lastLocation.longitude).toString())
                            startActivity(intentData)
                        }
                        locationProvided=false
                    }
                }
                fusedLocationClient.requestLocationUpdates(reqSetting, locationUpdates, null)
            }.addOnFailureListener {e->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val rae: ResolvableApiException = e as ResolvableApiException
                        rae.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                    } catch (sie: IntentSender.SendIntentException) {
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {

                    }
                }
            }
        }
    }
}