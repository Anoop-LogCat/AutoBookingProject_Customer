package production.logcat.ayeautocustomer.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.drivercard.view.*
import production.logcat.ayeautocustomer.models.DriverModel
import production.logcat.ayeautocustomer.R
import production.logcat.ayeautocustomer.models.customerObject
import production.logcat.ayeautocustomer.models.dialogAlertBox
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DriverAdapter(private val driverList: List<DriverModel>) : RecyclerView.Adapter<DriverAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.drivercard, parent, false)
        return ViewHolder(v)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(driverList[position])
    }
    override fun getItemCount(): Int {
        return driverList.size
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(driver: DriverModel) {
            val driverName: TextView =itemView.findViewById(R.id.driverName)
            val driverDistance: TextView =itemView.findViewById(R.id.driverDistance)
            val driverAge: TextView =itemView.findViewById(R.id.driverAge)
            val driverTime: TextView =itemView.findViewById(R.id.driverTime)
            val driverPhone: TextView =itemView.findViewById(R.id.driverPhone)
            val profile:ImageView = itemView.findViewById(R.id.profile_drivers)
            val book: Button =itemView.findViewById(R.id.bookNowButton)
            book.setOnClickListener {
                if((driver.driverDistance).toDouble()<=5){
                    messageDialogBox(itemView,driver)
                }
                else{
                    dialogAlertBox(itemView.resources.getDrawable(R.drawable.tagdialogbackgroundstyle), itemView.context, "Oops !!", "Sorry... Rickshaw driver is too away to make a pick up try another one", "OK", "", R.drawable.warning, okFunc = {}, cancelFunc = {}, okVisible = true, cancelVisible = false)
                }
            }
            val capitalized = StringBuilder(driver.driverName)
            capitalized.setCharAt(0, Character.toUpperCase(capitalized[0]))
            if(driver.driverProfile=="no image"){
                profile.setImageDrawable(itemView.resources.getDrawable(R.drawable.defaultdriver))
            }
            else{
                Picasso.get().load(driver.driverProfile).resize(1100,1300).centerCrop().into(profile)
            }
            driverName.text=capitalized.toString()
            driverDistance.text=when(driver.driverDistance.compareTo("0")==0){
                true->"less than 1 km"
                else->"${driver.driverDistance} km Away"
            }
            driverAge.text="Age : ${driver.driverAge} Years"
            driverTime.text="Working Time : ${driver.driverTime}"
            driverPhone.text=driver.driverPhone
        }


        private fun messageDialogBox(view:View,driverObject: DriverModel){
            val mDialogView = LayoutInflater.from(view.context).inflate(R.layout.tagbookinglayout, null)
            val mBuilder = MaterialAlertDialogBuilder(view.context).setView(mDialogView)
            mBuilder.background = view.resources.getDrawable(R.drawable.tagdialogbackgroundstyle)
            val dialog=mBuilder.show()
            val dialogTitle:TextView=mDialogView.findViewById(R.id.dialogTitle)
            val dialogImage:ImageView=mDialogView.findViewById(R.id.dialogTitleImage)
            val dialogBody:TextView=mDialogView.findViewById(R.id.dialogBody)
            val dialogOk:Button=mDialogView.findViewById(R.id.okButton)
            val dialogCancel:Button=mDialogView.findViewById(R.id.cancelButton)
            dialogTitle.text="Safety Alert"
            dialogBody.text="This option will send the drivers details to your nominee if you provided"
            dialogImage.setImageResource(R.drawable.nomineemsgsend)
            dialogCancel.visibility=View.VISIBLE
            dialogOk.visibility = View.VISIBLE
            dialogOk.text="Send & Book"
            dialogCancel.text="Skip & Book"
            dialogCancel.setOnClickListener {
                dialog.dismiss()
                driverObject.bookWithoutSendMessage(driverObject)
            }
            dialogOk.setOnClickListener {
                dialog.dismiss()
                val arrayAdapter = ArrayAdapter<String>(itemView.context, android.R.layout.simple_list_item_1)
                customerObject!!.nominee.keys.forEach{ id ->
                    if(customerObject!!.nominee[id].toString().compareTo("1234567890")!=0){
                        arrayAdapter.add("$id : ${customerObject!!.nominee[id].toString()}")
                    }
                }
                if(!arrayAdapter.isEmpty){
                    val builderSingle: AlertDialog.Builder = AlertDialog.Builder(itemView.context)
                    builderSingle.setTitle("Select Nominee")
                    builderSingle.setNegativeButton("cancel") { dialog, _ -> dialog.dismiss() }
                    builderSingle.setAdapter(arrayAdapter) { dialog, which ->
                        dialog.dismiss()
                        val strName = arrayAdapter.getItem(which)!!.split(":").last().removePrefix(" ")
                        driverObject.bookWithSendMessage(driverObject,strName)
                    }
                    builderSingle.show()
                }
                else{
                    Toast.makeText(itemView.context,"no nominee number added",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}