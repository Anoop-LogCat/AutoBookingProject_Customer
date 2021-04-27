package production.logcat.ayeautocustomer

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import production.logcat.ayeautocustomer.models.BookingModel
import production.logcat.ayeautocustomer.models.dialogAlertBox

class Home : Fragment() {

    private lateinit var trackLayout:RelativeLayout
    private lateinit var trackButton:Button
    private lateinit var trackCloseButton:ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navView: BottomNavigationView = view.findViewById(R.id.bottomNav)
        val navController: NavController = activity?.let { Navigation.findNavController(it, R.id.fragment2) }!!
        NavigationUI.setupWithNavController(navView, navController)

        trackLayout=view.findViewById(R.id.map_push_layout)
        trackButton=view.findViewById(R.id.trackButton)
        trackCloseButton=view.findViewById(R.id.close_track)
        trackLayout.visibility=View.GONE
        checkIsBooked()

        trackCloseButton.setOnClickListener {
            trackLayout.animate().alpha(0.0f).setDuration(500).withEndAction {
                trackLayout.visibility=View.GONE
            }
        }
    }

    private fun checkIsBooked(){
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val bookingObject = Gson().fromJson(Gson().toJson(dataSnapshot.value), BookingModel::class.java)
                if(bookingObject.driverName.compareTo("demoName")==0){
                   trackLayout.visibility=View.GONE
                }
                else{
                    trackLayout.visibility=View.VISIBLE
                    trackButton.setOnClickListener{
                        requireContext().startActivity(Intent(requireContext(),MapActivity::class.java))
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "no booking", Toast.LENGTH_SHORT).show()
            }
        }
        FirebaseDatabase.getInstance().reference.child("CustomerBooking").child(FirebaseAuth.getInstance().currentUser!!.uid).addValueEventListener(postListener)
    }
}