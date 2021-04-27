package production.logcat.ayeautocustomer.models

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import production.logcat.ayeautocustomer.R

data class StandModel(val standName:String,val standDistance:String,val standLandMark:String,val nextPage:(String,String)->Unit)
data class DriverModel(val driverUid:String,val driverDistance:String, val driverProfile:String, val driverName:String, val driverAge:String, val driverTime:String, val driverPhone:String, val driverAutoNumber:String,val bookWithoutSendMessage:(DriverModel)->Unit, val bookWithSendMessage:(DriverModel, String)->Unit)
data class HistoryModel(val driverName:String,val historyDate:String,val historyFrom:String,val historyTo:String,val historyAmount:String)
data class DriverUPIData(val DriverID: String, val UPIUsername: String, val UPICode: String, val Currency: String)
data class LocationClass(val sharing: Boolean, val lat: String, val long: String)
data class FareTable(val minimumCharge: String, val minimumDistance: String, val addCharge: String)
data class BookingModel(val driverName: String, val driverPhone: String, val driverUid: String, val driverProfile: String, val fare: String, val startLat: String, val startLong: String, val endLat: String, val endLong: String)
data class MapModel(val driverName: String, val driverPhone: String, val driverUid: String, val driverProfile: String, val initialLat: String, val initialLong: String)

var villageName:String="Select Your Location"
var locationLandMark:String="your location landmark address will be display here"

var phoneLogin:String="phone"
var usernameSignUp:String="username"
var phoneSignUp:String="phone"

var locationProvided:Boolean=false

var customerObject: FireCustomerModel?=null
var allStandDataJSON:String?=null

fun dialogAlertBox(background:Drawable,context: Context, title: String, bodyValue: String, okText: String, cancelText: String, iconID: Int, okFunc: () -> Unit, cancelFunc: () -> Unit, okVisible: Boolean, cancelVisible: Boolean){
    val mDialogView = LayoutInflater.from(context).inflate(R.layout.tagbookinglayout, null)
    val mBuilder = MaterialAlertDialogBuilder(context).setView(mDialogView)
    mBuilder.background = background
    mBuilder.setCancelable(false)
    val dialog=mBuilder.show()
    val dialogTitle: TextView =mDialogView.findViewById(R.id.dialogTitle)
    val dialogImage: ImageView =mDialogView.findViewById(R.id.dialogTitleImage)
    val dialogBody: TextView =mDialogView.findViewById(R.id.dialogBody)
    val dialogOk: Button =mDialogView.findViewById(R.id.okButton)
    val dialogCancel: Button =mDialogView.findViewById(R.id.cancelButton)
    if(okVisible&&cancelVisible){
        dialogOk.visibility= View.VISIBLE
        dialogCancel.visibility= View.VISIBLE
    }
    else if(okVisible&&!cancelVisible){
        dialogOk.visibility= View.VISIBLE
        dialogCancel.visibility= View.GONE
    }
    else if(!okVisible&&cancelVisible){
        dialogOk.visibility= View.INVISIBLE
        dialogCancel.visibility= View.VISIBLE
    }
    dialogTitle.text=title
    dialogBody.text=bodyValue
    dialogImage.setImageResource(iconID)
    dialogOk.text=okText
    dialogCancel.text=cancelText
    dialogCancel.setOnClickListener {
        dialog.cancel()
        cancelFunc()
    }
    dialogOk.setOnClickListener {
        okFunc()
        dialog.cancel()
    }
}