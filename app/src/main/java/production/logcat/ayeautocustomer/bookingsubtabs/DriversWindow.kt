package production.logcat.ayeautocustomer.bookingsubtabs

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.wang.avi.AVLoadingIndicatorView
import io.paperdb.Paper
import okhttp3.OkHttpClient
import okhttp3.Request
import production.logcat.ayeautocustomer.*
import production.logcat.ayeautocustomer.adapters.DriverAdapter
import production.logcat.ayeautocustomer.models.DriverModel
import production.logcat.ayeautocustomer.models.FireDriverModel
import production.logcat.ayeautocustomer.models.customerObject
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DriversWindow : Fragment() {

    private lateinit var standNameTextView:TextView
    private lateinit var standLandMarkTextView:TextView
    private lateinit var driverRecycler: RecyclerView
    private lateinit var noDataLayout:LinearLayout
    private lateinit var subtitleTextView:TextView
    private lateinit var driverLoader:AVLoadingIndicatorView

    private val driverList=ArrayList<DriverModel>()

    private val argsInDriversWindow: DriversWindowArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_drivers_window, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        standNameTextView=view.findViewById(R.id.standNameTextView)
        standLandMarkTextView=view.findViewById(R.id.stanLandMarkTextView)
        driverRecycler=view.findViewById(R.id.driverListRecycler)
        subtitleTextView=view.findViewById(R.id.subtitle)
        noDataLayout=view.findViewById(R.id.noDataDisplayLayout_Drivers)
        subtitleTextView.visibility=View.INVISIBLE
        noDataLayout.visibility=View.INVISIBLE
        standNameTextView.text=argsInDriversWindow.standName
        standLandMarkTextView.text=argsInDriversWindow.standLandMark
        driverLoader=view.findViewById(R.id.driverLoader)
        standLandMarkTextView.isSelected=true
        GetStandDriverInfo().execute(argsInDriversWindow.standName)
    }


    private fun noDriverLayoutFunc(mode:String){
        when(mode){
            "NO_DRIVER"->{
                subtitleTextView.visibility=View.GONE
                driverRecycler.visibility=View.GONE
                noDataLayout.visibility=View.VISIBLE
                driverLoader.hide()
            }
            "YES_DRIVER"->{
                driverRecycler.visibility=View.VISIBLE
                subtitleTextView.visibility=View.VISIBLE
                noDataLayout.visibility=View.GONE
                driverLoader.hide()
            }
            "LOADING"->{
                driverRecycler.visibility=View.GONE
                subtitleTextView.visibility=View.GONE
                noDataLayout.visibility=View.GONE
                driverLoader.show()
            }
        }
    }

    private fun getDriverDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(lon1 - lon2)
        val a = (sin(latDistance / 2) * sin(latDistance / 2) + (cos(Math.toRadians(lat1)) * cos(
                Math.toRadians(
                        lat2
                )
        ) * sin(lngDistance / 2) * sin(lngDistance / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (6371 * c)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    private inner class GetStandDriverInfo : AsyncTask<String, Void, String>(){
        override fun onPreExecute() {
            super.onPreExecute()
            noDriverLayoutFunc("LOADING")
        }
        override fun doInBackground(vararg params: String?): String {
            return try {
                val url = "https://us-central1-auto-pickup-apps.cloudfunctions.net/CustomerDriverView/${params[0]!!}"
                val request: Request = Request.Builder().url(url).build()
                val client = OkHttpClient()
                val response= client.newCall(request).execute()
                response.body()?.string().toString()
            }catch (e: Exception){
                "ERROR"
            }
        }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            when {
                result!!.compareTo("no data")==0 -> {
                    noDriverLayoutFunc("NO_DRIVER")
                }
                result.compareTo("ERROR")==0 -> {
                    Toast.makeText(context, "no network", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    noDriverLayoutFunc("YES_DRIVER")
                    if(driverList.isNotEmpty()){driverList.clear()}
                    val allDriverDoc: java.util.HashMap<*, *>? = Gson().fromJson(result, HashMap::class.java)
                    allDriverDoc?.keys?.forEach {
                        val eachDriverData = Gson().fromJson(Gson().toJson(allDriverDoc[it.toString()]), FireDriverModel::class.java)
                        val driverDistance = getDriverDistance((eachDriverData.driverLat.toString()).toDouble(),(eachDriverData.driverLong.toString()).toDouble(),argsInDriversWindow.currentLatitude.toDouble(),argsInDriversWindow.currentLongitude.toDouble())
                        driverList.add(
                            DriverModel(it.toString(),(driverDistance.toInt()).toString(),eachDriverData.imageUrl, eachDriverData.username, eachDriverData.age, eachDriverData.workingTime, eachDriverData.phone, eachDriverData.autoNumber,bookWithSendMessage = { driverDetails, nomineeNumber ->
                                    if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                        if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.SEND_SMS)) {
                                            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.SEND_SMS), 147)
                                        } else {
                                            Toast.makeText(context, "permission denied", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val sendSMS = SmsManager.getDefault()
                                        sendSMS.sendTextMessage(
                                            nomineeNumber,
                                            null,
                                            "Aye Auto \n\nDriver Name : ${driverDetails.driverName}\nPhone Number : ${driverDetails.driverPhone}\nAge : ${driverDetails.driverAge}\nAuto Number : ${driverDetails.driverAutoNumber}\n\nThis details has been send by ${customerObject!!.username}",
                                            null,
                                            null
                                        )
                                        Toast.makeText(context, "message has been send successfully", Toast.LENGTH_SHORT).show()
                                        val intentData = Intent(requireActivity(), MapActivity::class.java)
                                        intentData.putExtra("driverName", driverDetails.driverName)
                                        intentData.putExtra("driverPhone", driverDetails.driverPhone)
                                        intentData.putExtra("driverUid", driverDetails.driverUid)
                                        intentData.putExtra("driverProfile", driverDetails.driverProfile)
                                        intentData.putExtra("initialLatitude", argsInDriversWindow.currentLatitude)
                                        intentData.putExtra("initialLongitude", argsInDriversWindow.currentLongitude)
                                        startActivity(intentData)
                                    }
                            },bookWithoutSendMessage = {driverDetails->
                                val intentData = Intent(requireActivity(), MapActivity::class.java)
                                intentData.putExtra("driverName", driverDetails.driverName)
                                intentData.putExtra("driverPhone", driverDetails.driverPhone)
                                intentData.putExtra("driverUid", driverDetails.driverUid)
                                intentData.putExtra("driverProfile", driverDetails.driverProfile)
                                intentData.putExtra("initialLatitude", argsInDriversWindow.currentLatitude)
                                intentData.putExtra("initialLongitude", argsInDriversWindow.currentLongitude)
                                startActivity(intentData)
                            })
                        )
                    }
                    val sortedDriverList = driverList.sortedBy { it.driverDistance }
                    val adapter=DriverAdapter(sortedDriverList)
                    driverRecycler.layoutManager=LinearLayoutManager(requireActivity(),RecyclerView.VERTICAL,false)
                    driverRecycler.adapter=adapter
                }
            }
        }
    }
}