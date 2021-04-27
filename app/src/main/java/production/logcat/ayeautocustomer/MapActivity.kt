package production.logcat.ayeautocustomer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import production.logcat.ayeautocustomer.models.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapActivity : AppCompatActivity() , OnMapReadyCallback,MapboxMap.OnMapClickListener,ValueEventListener {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val sourceRequestCode = 3
    private val destinationRequestCode = 4

    private lateinit var mapbox: MapView
    private lateinit var sourceEditText: EditText
    private lateinit var destinationEditText: EditText
    private lateinit var driverLayout: MaterialCardView
    private lateinit var locationSetter: FrameLayout
    private lateinit var driverPic: CircleImageView
    private lateinit var driverNameWithDescription: TextView
    private lateinit var callDriver: ImageView
    private lateinit var driverStatus: TextView
    private lateinit var driverDistanceTextView: TextView
    private lateinit var paymentButton:Button
    private lateinit var saveButton:Button
    private lateinit var bookButton:Button
    private lateinit var sourceEdit:ImageButton
    private lateinit var destinationEdit:ImageButton

    private var destinationMarker: Marker?=null
    private var sourceMarker: Marker?=null
    private var driverMarker: Marker?=null
    private lateinit var map: MapboxMap
    private var user:FirebaseUser?=null
    private var navigationMapRoute: NavigationMapRoute? = null

    private var cityName:String?=null
    private var driverUidValue:String?=null
    private var fareRate:String="0.0"
    private var isBooked:Boolean=false
    private var oneTime:Int=1
    private var profileDrawable:Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Mapbox.getInstance(this, resources.getString(R.string.MAP_BOX_API_KEY))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        user=FirebaseAuth.getInstance().currentUser
        profileDrawable=resources.getDrawable(R.drawable.defaultdriver)
        mapbox = findViewById(R.id.mapBoxMapInMapFragment)
        mapbox.onCreate(savedInstanceState)
        mapbox.getMapAsync(this)

        sourceEditText=findViewById(R.id.sourceTextView)
        destinationEditText=findViewById(R.id.destinationTextView)
        driverLayout=findViewById(R.id.driverLayout)
        driverPic=findViewById(R.id.driverPic)
        sourceEdit=findViewById(R.id.sourceEditButton)
        destinationEdit=findViewById(R.id.destinationEditButton)
        locationSetter=findViewById(R.id.locationSettingLayout)
        driverNameWithDescription=findViewById(R.id.driverNameDescription)
        callDriver=findViewById(R.id.callButton)
        driverStatus=findViewById(R.id.driverStatus)
        driverDistanceTextView=findViewById(R.id.distance)
        paymentButton=findViewById(R.id.payment)
        bookButton=findViewById(R.id.bookingButton)
        saveButton=findViewById(R.id.saveButton)
        layoutMode("LOADING")

        bookButton.setOnClickListener {
            if(destinationMarker!=null&&sourceMarker!=null&&destinationEditText.text.isNotEmpty()&&sourceEditText.text.isNotEmpty()){
                getRoute(Point.fromLngLat(sourceMarker!!.position.longitude, sourceMarker!!.position.latitude), Point.fromLngLat(destinationMarker!!.position.longitude, destinationMarker!!.position.latitude),false)
            }
            else{
                Toast.makeText(baseContext, "please provide route", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeDriverData():MapModel{
        return MapModel(
                intent.getStringExtra("driverName").toString(),
                intent.getStringExtra("driverPhone").toString(),
                intent.getStringExtra("driverUid").toString(),
                intent.getStringExtra("driverProfile").toString(),
                intent.getStringExtra("initialLatitude").toString(),
                intent.getStringExtra("initialLongitude").toString()
        )
    }

    private fun layoutMode(mode: String){
        when(mode){
            "BOOKING_WINDOW" -> {
                driverLayout.visibility = View.GONE
                locationSetter.visibility = View.VISIBLE
            }
            "LOADING" -> {
                driverLayout.visibility = View.GONE
                locationSetter.visibility = View.GONE
            }
            else->{
                isBooked=true
                driverLayout.visibility= View.VISIBLE
                locationSetter.visibility= View.GONE
                database.child("DriversNavigation").child(driverUidValue!!).addValueEventListener(this)
            }
        }
    }

    private fun buttonActivation(activationButton: Button, isActive: Boolean){
        activationButton.isClickable=isActive
    }

    private fun setSourceMarker(point: LatLng){
        try{
            val geoCoder = Geocoder(this, Locale.getDefault())
            val initialAddress: List<Address>? = geoCoder.getFromLocation(initializeDriverData().initialLat.toDouble(), initializeDriverData().initialLong.toDouble(), 1)
            val initialCity = initialAddress?.get(0)?.subAdminArea
            val markedAddress: List<Address>? = geoCoder.getFromLocation(point.latitude, point.longitude, 1)
            val markedCity = markedAddress?.get(0)?.subAdminArea
            if(initialCity!!.compareTo(markedCity!!)==0){
                if(sourceMarker!=null){ map.removeMarker(sourceMarker!!)}
                sourceMarker = map.addMarker(MarkerOptions().position(point))
                sourceMarker!!.icon= IconFactory.recreate(resources.getString(R.string.SOURCE_ICON_ID), BitmapFactory.decodeResource(resources, R.drawable.source_icon))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15.0))
                getCompleteAddressString(point, sourceEditText)
            }
            else{
                Toast.makeText(this@MapActivity, "Can't change district", Toast.LENGTH_SHORT).show()
            }
        }catch (e: java.lang.Exception){

        }
    }

    private fun setDestinationMarker(point: LatLng){
        if(destinationMarker!=null){ map.removeMarker(destinationMarker!!)}
        destinationMarker = map.addMarker(MarkerOptions().position(point))
        destinationMarker!!.icon= IconFactory.recreate(resources.getString(R.string.DESTINATION_ICON_ID), BitmapFactory.decodeResource(resources, R.drawable.destination_icon))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15.0))
        getCompleteAddressString(point, destinationEditText)
    }

    private fun setDriverMarker(point: LatLng){
        if(driverMarker!=null){ map.removeMarker(driverMarker!!) }
        driverMarker = map.addMarker(MarkerOptions().position(point))
        driverMarker!!.title="Driver"
        driverMarker!!.icon= IconFactory.recreate(resources.getString(R.string.DRIVER_ICON_ID), BitmapFactory.decodeResource(resources, R.drawable.autolocationicon))
    }

    private fun removeLocations(){
        map.removeMarker(destinationMarker!!)
        map.removeMarker(sourceMarker!!)
        sourceEditText.setText("")
        destinationEditText.setText("")
        destinationMarker=null
        sourceMarker=null
    }

    private fun typeLocationWindow(code: Int){
        val intent: Intent = PlaceAutocomplete.IntentBuilder()
            .accessToken(resources.getString(R.string.MAP_BOX_API_KEY))
            .placeOptions(
                    PlaceOptions.builder()
                            .backgroundColor(Color.parseColor("#EEEEEE"))
                            .limit(10)
                            .build(PlaceOptions.MODE_CARDS))
            .build(this@MapActivity)
        startActivityForResult(intent, code)
    }

    private fun changeDriverStatus(){
        if(isBooked){
            var temp:Int=1
            val postListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val driverStatusMap: HashMap<*, *>? = Gson().fromJson(Gson().toJson(dataSnapshot.value), HashMap::class.java)
                    if(driverStatusMap!!["status"].toString().compareTo("Cancelled")==0&&temp==1){
                        temp++
                        driverStatus.text=driverStatusMap["status"].toString()
                        dialogAlertBox(resources.getDrawable(R.drawable.tagdialogbackgroundstyle), this@MapActivity, "Booking Rejected", "Sorry... Your driver has rejected your booking try with another driver", "OK", "", R.drawable.auto, okFunc = { finish() }, cancelFunc = {}, okVisible = true, cancelVisible = false)
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(baseContext, "not changed", Toast.LENGTH_SHORT).show()
                }
            }
            database.child("DriversStatus").child(driverUidValue!!).addValueEventListener(postListener)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getCompleteAddressString(pos: LatLng, editText: EditText){
        val geoCoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? = geoCoder.getFromLocation(pos.latitude, pos.longitude, 1)
            if (addresses != null) {
                val road = addresses[0].thoroughfare
                val panchayath = addresses[0].locality
                val gilla =addresses[0].subAdminArea
                val state = addresses[0].adminArea
                if(panchayath.compareTo("null")!=0&&gilla.compareTo("null")!=0&&state.compareTo("null")!=0){
                    if(editText.id==sourceEditText.id){
                        cityName=gilla
                        editText.setText("$road $panchayath $gilla $state")
                    }
                    else{
                        editText.setText("$road $panchayath $gilla $state")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPermissionAndCall(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 29)
            }
            else{
                Toast.makeText(baseContext, "permission denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            startActivity(intent)
        }
    }

    private fun saveTravelHistory(fare: String, name: String){
        buttonActivation(saveButton, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            customerObject!!.history[System.currentTimeMillis().toString()] = Gson().toJson(mapOf(
                    "historyFrom" to sourceEditText.text.toString(),
                    "historyTo" to destinationEditText.text.toString(),
                    "historyDate" to LocalDate.now().toString(),
                    "driverName" to name,
                    "historyAmount" to fare
            ))
        }
        FirebaseFunctions.getInstance().getHttpsCallable("UpdateCustomer").call(mapOf(
                "userID" to FirebaseAuth.getInstance().currentUser!!.uid,
                "key" to "history",
                "value" to customerObject!!.history
        )).addOnCompleteListener { isChanged ->
            buttonActivation(saveButton, true)
            when (!isChanged.isSuccessful) {
                true -> Toast.makeText(this, "save failed", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateFare(distance: Double, fareObject: FareTable):String{
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(if ((fareObject.minimumDistance).toDouble() >= distance) {
            (fareObject.minimumCharge).toDouble()
        } else {
            val extraDistance = ((distance - (fareObject.minimumDistance).toDouble()))
            fareObject.minimumCharge.toDouble() + ((fareObject.addCharge.toDouble()) * extraDistance)
        })
    }

    private fun getDistanceFromLatLonInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(lon1 - lon2)
        val a = (sin(latDistance / 2) * sin(latDistance / 2) + (cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(lngDistance / 2) * sin(lngDistance / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format((6371 * c))
    }

    private fun setCustomerBookingData(amount: String){
        val bookingObject = BookingModel(initializeDriverData().driverName, initializeDriverData().driverPhone, initializeDriverData().driverUid, initializeDriverData().driverProfile, fareRate, (sourceMarker!!.position.latitude).toString(), (sourceMarker!!.position.longitude).toString(), (destinationMarker!!.position.latitude).toString(), (destinationMarker!!.position.longitude).toString())
        database.child("CustomerBooking").child(user!!.uid).setValue(bookingObject).addOnCompleteListener {
            if(!it.isSuccessful){
                Toast.makeText(baseContext, "Failed to upload booking data", Toast.LENGTH_SHORT).show()
            }
            else{
                setData(amount, initializeDriverData().driverName, initializeDriverData().driverProfile, initializeDriverData().driverPhone)
            }
        }
    }

    private fun getCustomerBookingData(){
        val progressBarInMap = ProgressDialog(this@MapActivity)
        progressBarInMap.setCancelable(false)
        progressBarInMap.setMessage("Loading")
        progressBarInMap.show()
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val bookingObject = Gson().fromJson(Gson().toJson(dataSnapshot.value), BookingModel::class.java)
                if(bookingObject.driverName.compareTo("demoName")!=0){
                    if(intent.getStringExtra("driverUid")!=null){
                        if(intent.getStringExtra("driverUid").toString().compareTo(bookingObject.driverUid)==0&&oneTime==1){
                            progressBarInMap.cancel()
                            setBookingData(bookingObject)
                        }else if(intent.getStringExtra("driverUid").toString().compareTo(bookingObject.driverUid)!=0&&oneTime==1){
                            progressBarInMap.cancel()
                            oneTime=23
                            dialogAlertBox(
                                    resources.getDrawable(R.drawable.tagdialogbackgroundstyle),
                                    this@MapActivity,
                                    "Warning",
                                    "You already have a booked auto rickshaw driver",
                                    "OK", "NO",
                                    R.drawable.warning, okFunc = { finish() }, cancelFunc = {},
                                    okVisible = true,
                                    cancelVisible = false
                            )
                        }
                    }else if(intent.getStringExtra("driverUid")==null&&oneTime==1){
                        progressBarInMap.cancel()
                        setBookingData(bookingObject)
                    }
                }
                else if(bookingObject.driverName.compareTo("demoName")==0&&oneTime==1){
                    progressBarInMap.cancel()
                    oneTime=23
                    driverUidValue=initializeDriverData().driverUid
                    layoutMode("BOOKING_WINDOW")
                    sourceEdit.setOnClickListener { typeLocationWindow(sourceRequestCode) }
                    destinationEdit.setOnClickListener { typeLocationWindow(destinationRequestCode) }
                    setSourceMarker(LatLng(initializeDriverData().initialLat.toDouble(), initializeDriverData().initialLong.toDouble()))
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(baseContext, "cant access booking data", Toast.LENGTH_SHORT).show()
            }
        }
        database.child("CustomerBooking").child(user!!.uid).addValueEventListener(postListener)
    }

    private fun setBookingData(bookingObject: BookingModel){
        oneTime=23
        driverUidValue=bookingObject.driverUid
        fareRate=bookingObject.fare
        sourceMarker = map.addMarker(MarkerOptions().position(LatLng((bookingObject.startLat).toDouble(),(bookingObject.startLong).toDouble())))
        sourceMarker!!.icon= IconFactory.recreate(resources.getString(R.string.SOURCE_ICON_ID), BitmapFactory.decodeResource(resources, R.drawable.source_icon))
        destinationMarker = map.addMarker(MarkerOptions().position(LatLng((bookingObject.endLat).toDouble(),(bookingObject.endLong).toDouble())))
        destinationMarker!!.icon= IconFactory.recreate(resources.getString(R.string.DESTINATION_ICON_ID), BitmapFactory.decodeResource(resources, R.drawable.destination_icon))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng((bookingObject.endLat).toDouble(),(bookingObject.endLong).toDouble()), 15.0))
        getRoute(Point.fromLngLat(sourceMarker!!.position.longitude, sourceMarker!!.position.latitude), Point.fromLngLat(destinationMarker!!.position.longitude, destinationMarker!!.position.latitude),true)
        setData(fareRate, bookingObject.driverName, bookingObject.driverProfile, bookingObject.driverPhone)
        getCompleteAddressString(LatLng((bookingObject.startLat).toDouble(),(bookingObject.startLong).toDouble()),sourceEditText)
        getCompleteAddressString(LatLng((bookingObject.endLat).toDouble(),(bookingObject.endLong).toDouble()),destinationEditText)
    }

    private fun getRoute(originPoint: Point, endPoint: Point,again:Boolean) {
        buttonActivation(bookButton, false)
        map.removeOnMapClickListener(this)
        NavigationRoute.builder(this@MapActivity).accessToken(resources.getString(R.string.MAP_BOX_API_KEY)).origin(originPoint).destination(endPoint).build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse?>, response: Response<DirectionsResponse?>) {
                        if (response.body() == null) { return }
                        else if (response.body()!!.routes().size == 0) { return }
                        val currentRoute = response.body()!!.routes()[0]
                        if (navigationMapRoute != null) { navigationMapRoute?.updateRouteVisibilityTo(false) }
                        else {
                            navigationMapRoute = NavigationMapRoute(null, mapbox, map)
                            navigationMapRoute!!.addRoute(currentRoute)
                            if (!again) {
                                val postListener = object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val fareObject = Gson().fromJson(Gson().toJson(dataSnapshot.value), FareTable::class.java)
                                        fareRate = generateFare((currentRoute.legs()!![0]?.distance()!! / 1000), fareObject)
                                        dialogAlertBox(
                                                resources.getDrawable(R.drawable.tagdialogbackgroundstyle),
                                                this@MapActivity,
                                                "Fare Rate Rs. $fareRate",
                                                "The fare rate for travel is Rs. $fareRate. This may change according to driver's arrival distance or due to bad road condition or according to the time (night)",
                                                "Book Now", "Cancel", R.drawable.fare_icon,
                                                okFunc = {
                                                    uploadBooking(fareRate)
                                                         },
                                                cancelFunc = {
                                                    buttonActivation(bookButton, true)
                                                    map.addOnMapClickListener(this@MapActivity)
                                                }, okVisible = true, cancelVisible = true)
                                    }
                                    override fun onCancelled(databaseError: DatabaseError) {
                                        buttonActivation(bookButton, true)
                                        map.addOnMapClickListener(this@MapActivity)
                                        Toast.makeText(baseContext, "Fare not provided", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                database.child("FareTable").child(cityName!!).addValueEventListener(postListener)
                            }
                        }
                    }
                    override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
                        buttonActivation(bookButton, true)
                        map.addOnMapClickListener(this@MapActivity)
                    }
                })

    }

    private fun uploadBooking(amount: String){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            FirebaseFunctions.getInstance().getHttpsCallable("CustomerBooking").call(
                    mapOf(
                            "customerData" to mapOf<String, Any>(
                                    "cusLatitude" to sourceMarker!!.position.latitude,
                                    "cusLongitude" to sourceMarker!!.position.longitude,
                                    "customerLocation" to "From : ${sourceEditText.text}_To : ${destinationEditText.text}",
                                    "customerDate" to "${
                                        if (LocalDateTime.now().dayOfMonth.toString().length == 1) {
                                            "0${LocalDateTime.now().dayOfMonth}"
                                        } else {
                                            LocalDateTime.now().dayOfMonth
                                        }
                                    }-" +
                                            "${
                                                if (LocalDateTime.now().monthValue.toString().length == 1) {
                                                    "0${LocalDateTime.now().monthValue}"
                                                } else {
                                                    LocalDateTime.now().monthValue
                                                }
                                            }-" +
                                            "${LocalDateTime.now().year}:" +
                                            "${
                                                if (LocalDateTime.now().hour.toString().length == 1) {
                                                    "0${LocalDateTime.now().hour}"
                                                } else {
                                                    LocalDateTime.now().hour
                                                }
                                            }:" +
                                            "${
                                                if (LocalDateTime.now().minute.toString().length == 1) {
                                                    "0${LocalDateTime.now().minute}"
                                                } else {
                                                    LocalDateTime.now().minute
                                                }
                                            }",
                                    "customerName" to customerObject!!.username,
                                    "customerNumber" to customerObject!!.phone,
                                    "from" to FirebaseAuth.getInstance().currentUser!!.uid,
                            ),
                            "driverUid" to initializeDriverData().driverUid,
                            "fare" to amount
                    )
            ).addOnCompleteListener { isChanged ->
                when (!isChanged.isSuccessful) {
                    true -> {
                        buttonActivation(bookButton, true)
                        map.addOnMapClickListener(this)
                        Toast.makeText(baseContext, "booking failed", Toast.LENGTH_SHORT).show()
                    }
                    else-> {
                        if((isChanged.result!!.data.toString()).compareTo("driver busy")==0){
                            dialogAlertBox(resources.getDrawable(R.drawable.tagdialogbackgroundstyle), this@MapActivity, "Busy Driver", "Sorry... Your auto driver currently running for some another customer lets try with another driver", "OK", "", R.drawable.defaultdriver, okFunc = { finish() }, cancelFunc = {}, okVisible = true, cancelVisible = false)
                        }
                        else{
                            dialogAlertBox(resources.getDrawable(R.drawable.tagdialogbackgroundstyle), this@MapActivity, "Booking Done", "Your auto booking has been successfully created", "OK", "", R.drawable.checked, okFunc = { locationSetter.animate().alpha(0.0f).setDuration(300).withEndAction {
                                setCustomerBookingData(amount)
                            } }, cancelFunc = {}, okVisible = true, cancelVisible = false)
                        }
                    }
                }
            }
        }
    }

    private fun setData(amount: String, name: String, profile: String, phone: String){
        layoutMode("DRIVER_WINDOW")
        val driverNameWithDescriptionString="$name ${resources.getString(R.string.driverDescription)}"
        when(profile=="no image"){
            true -> driverPic.setImageDrawable(profileDrawable)
            else->Picasso.get().load(profile).into(driverPic)
        }
        driverStatus.text="Driving"
        driverNameWithDescription.text=driverNameWithDescriptionString
        callDriver.setOnClickListener { checkPermissionAndCall(phone) }
        saveButton.setOnClickListener {
            buttonActivation(saveButton, false)
            dialogAlertBox(
                    resources.getDrawable(R.drawable.tagdialogbackgroundstyle),
                    this@MapActivity,
                    "Save Travel",
                    "Do you want to save the travel history",
                    "YES", "NO",
                    R.drawable.save_icon,
                    okFunc = { saveTravelHistory(amount, name) }, cancelFunc = { buttonActivation(saveButton, true) }, okVisible = true, cancelVisible = true
            )
        }
        paymentButton.setOnClickListener {
            val intent = IntentIntegrator(this@MapActivity)
            intent.setPrompt("For flash light use volume Up")
            intent.setBeepEnabled(true)
            intent.setOrientationLocked(false)
            intent.captureActivity=Capture::class.java
            intent.initiateScan()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun paymentDialogBox(name: String, code: String, currency: String){
        val mDialogView = LayoutInflater.from(this@MapActivity).inflate(R.layout.paymentfieldlayout, null)
        val mBuilder = MaterialAlertDialogBuilder(this@MapActivity).setView(mDialogView)
        mBuilder.background = resources.getDrawable(R.drawable.tagdialogbackgroundstyle)
        mBuilder.setCancelable(false)
        val dialog=mBuilder.show()
        val amount:EditText = mDialogView.findViewById(R.id.amountEditText)
        val note:EditText = mDialogView.findViewById(R.id.noteEditText)
        amount.setText(fareRate)
        note.setText("fare amount send by ${customerObject!!.username}")
        val dialogOk: Button =mDialogView.findViewById(R.id.okPayButton)
        val dialogCancel: Button =mDialogView.findViewById(R.id.cancelPayButton)
        dialogCancel.setOnClickListener {
            dialog.cancel()
        }
        dialogOk.setOnClickListener {
            if(amount.text.isNullOrBlank()||note.text.isNullOrBlank()){
                Toast.makeText(this@MapActivity, "invalid fields", Toast.LENGTH_SHORT).show()
            }
            else{
                if(fareRate.toDouble()<=amount.text.toString().toDouble()) {
                    fareRate = amount.text.toString()
                    val uri:Uri= Uri.parse("upi://pay").buildUpon()
                        .appendQueryParameter("pa", code)
                        .appendQueryParameter("pn", name)
                        .appendQueryParameter("tn", note.text.toString())
                        .appendQueryParameter("am", amount.text.toString())
                        .appendQueryParameter("cu", currency)
                        .build()
                    val upiPayIntent = Intent(Intent.ACTION_VIEW)
                    upiPayIntent.data = uri
                    val chooser:Intent= Intent.createChooser(upiPayIntent, "Pay With")
                    if(chooser.resolveActivity(packageManager)!=null){
                        startActivityForResult(chooser, 129)
                    }else{
                        Toast.makeText(this@MapActivity, "No UPI App found", Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    Toast.makeText(this@MapActivity, "fare is invalid", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun upiPaymentDataOperation(response: String, uid: String){
        val responseDataList=response.split("&")
        var status = ""
        responseDataList.forEach {
            val equalStr=it.split("=")
            if(equalStr.size>=2){
                if(equalStr[0].toLowerCase(Locale.ROOT).compareTo("Status".toLowerCase(Locale.ROOT)) == 0) {
                    status = equalStr[1].toLowerCase(Locale.ROOT)
                }
            }
            else{
                Toast.makeText(this@MapActivity, "Payment Cancelled by user", Toast.LENGTH_SHORT).show()
            }
        }
        if(status.compareTo("success")==0){
            dialogAlertBox(resources.getDrawable(R.drawable.tagdialogbackgroundstyle), this@MapActivity, "Payment Done", "Your payment of Rs $fareRate has been successfully completed", "OK", "", R.drawable.checked, okFunc = {}, cancelFunc = {}, okVisible = true, cancelVisible = false)
            FirebaseFunctions.getInstance().getHttpsCallable("mentionPayment").call(mapOf("driverUid" to uid)).addOnCompleteListener { payment ->
                when (payment.isSuccessful) {
                    true -> {
                        Toast.makeText(this@MapActivity, "message send to driver", Toast.LENGTH_SHORT).show()
                    }
                    false -> {
                        Toast.makeText(this@MapActivity, "message not send to driver", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        else{
            Toast.makeText(this@MapActivity, "Transaction Failed. Please try again", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        dialogAlertBox(
                resources.getDrawable(R.drawable.tagdialogbackgroundstyle),
                this@MapActivity,
                "Warning",
                "Are you sure for exiting the booking window",
                "YES", "NO",
                R.drawable.warning, okFunc = { finish() }, cancelFunc = {},
                okVisible = true,
                cancelVisible = true
        )
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapbox.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapbox.onResume()
        changeDriverStatus()
    }

    override fun onPause() {
        super.onPause()
        mapbox.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapbox.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapbox.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapbox.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapbox.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == sourceRequestCode) {
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)
            val point = LatLng((selectedCarmenFeature.geometry() as Point?)!!.latitude(), (selectedCarmenFeature.geometry() as Point?)!!.longitude())
            setSourceMarker(point)
        }
        else if (resultCode == Activity.RESULT_OK && requestCode == destinationRequestCode) {
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)
            val point = LatLng((selectedCarmenFeature.geometry() as Point?)!!.latitude(), (selectedCarmenFeature.geometry() as Point?)!!.longitude())
            setDestinationMarker(point)
        }
        else if (requestCode == 129) {
            if(resultCode == Activity.RESULT_OK || resultCode==11){
                if(data!=null){
                    val upiData:String=data.getStringExtra("response")!!
                    upiPaymentDataOperation(upiData, if (driverUidValue == null) {
                        initializeDriverData().driverUid
                    } else {
                        driverUidValue!!
                    })
                }else{
                    Toast.makeText(this@MapActivity, "payment cancelled", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this@MapActivity, "payment cancelled", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            try{
                val intentResult:IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
                if(intentResult.contents!=null){
                    val locationObject = Gson().fromJson(intentResult.contents, DriverUPIData::class.java)
                    if(locationObject.DriverID.compareTo(if (driverUidValue == null) {
                                initializeDriverData().driverUid
                            } else {
                                driverUidValue!!
                            })==0){
                        paymentDialogBox(locationObject.UPIUsername, locationObject.UPICode, locationObject.Currency)
                    }else{
                        Toast.makeText(this@MapActivity, "invalid QR Code", Toast.LENGTH_SHORT).show()
                    }
                }
            }catch (e: Exception){

            }
        }
    }

    override fun onMapClick(point: LatLng):Boolean {
        if(navigationMapRoute!=null) navigationMapRoute!!.removeRoute()
        if(destinationMarker!=null&&sourceMarker!=null) removeLocations()
        else if(destinationMarker==null&&sourceMarker!=null) setDestinationMarker(point)
        else if((destinationMarker!=null&&sourceMarker==null)||destinationMarker==null&&sourceMarker==null) setSourceMarker(point)
        return false
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        val locationObject = Gson().fromJson(Gson().toJson(snapshot.value), LocationClass::class.java)
        if(locationObject!!.sharing&&sourceMarker!=null){
            val distanceBetweenDriverAndCustomer = getDistanceFromLatLonInKm(sourceMarker!!.position.latitude, sourceMarker!!.position.longitude, locationObject.lat.toDouble(), locationObject.long.toDouble())
            setDriverMarker(LatLng(locationObject.lat.toDouble(), locationObject.long.toDouble()))
            driverDistanceTextView.text=when((distanceBetweenDriverAndCustomer).toDouble()<1.0){
                true -> "Driver will arrive soon..."
                else->"The driver is $distanceBetweenDriverAndCustomer km away"
            }
        }
        else{
            driverDistanceTextView.text=resources.getString(R.string.driverNotYet)
        }
    }
    override fun onCancelled(error: DatabaseError) {
        Toast.makeText(baseContext, "Failed to navigate", Toast.LENGTH_SHORT).show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        mapboxMap.addOnMapClickListener(this)
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            getCustomerBookingData()
        }
    }
}
