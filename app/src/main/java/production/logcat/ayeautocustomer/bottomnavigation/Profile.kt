package production.logcat.ayeautocustomer.bottomnavigation

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import com.wang.avi.AVLoadingIndicatorView
import de.hdodenhof.circleimageview.CircleImageView
import production.logcat.ayeautocustomer.MainActivity
import production.logcat.ayeautocustomer.R
import production.logcat.ayeautocustomer.models.customerObject
import java.io.ByteArrayOutputStream

class Profile : Fragment() {

    private lateinit var profileUserName:TextView
    private lateinit var privacyTextView:TextView
    private lateinit var profilePhoneNumber:TextView
    private lateinit var profileImage:CircleImageView
    private lateinit var profileImageProgress:AVLoadingIndicatorView
    private lateinit var addNominee:FrameLayout
    private lateinit var logOut:FrameLayout

    private val user = FirebaseAuth.getInstance().currentUser
    private val REQUEST_CODE = 52
    private val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(
        "gs://auto-pickup-apps.appspot.com"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profilePhoneNumber=view.findViewById(R.id.customerNumber)
        profileUserName=view.findViewById(R.id.customerName)
        profileImage=view.findViewById(R.id.profile_image)
        privacyTextView=view.findViewById(R.id.privacy_link)
        profileImageProgress=view.findViewById(R.id.progress_changing)
        addNominee=view.findViewById(R.id.add_nominee_button)
        logOut=view.findViewById(R.id.logOutButton)
        view.findViewById<TextView>(R.id.customerName).setOnClickListener {
            editingDialogBox()
        }
        view.findViewById<FrameLayout>(R.id.add_nominee_button).setOnClickListener {
            nomineeDialogBox()
        }
        view.findViewById<FrameLayout>(R.id.logOutButton).setOnClickListener {
           logOutDialogBox()
        }
        view.findViewById<ImageView>(R.id.changeProfilePicButton).setOnClickListener {
            checkPermissionAndPickImage()
        }
        privacyTextView.setOnClickListener {
            requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/10-Gsx0in_CWsp_fCUP0ia4K_YeVPmtMk/view?usp=drivesdk")))
        }
        view.findViewById<FrameLayout>(R.id.complains).setOnClickListener {
            val email = Intent(Intent.ACTION_SENDTO)
            email.data = Uri.parse("mailto:logcatsolutions@gmail.com")
            startActivity(email)
        }
        setData()
    }

    private fun logOutDialogBox(){
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Warning")
        builder.setMessage("Are you sure for log out ?")
        builder.setPositiveButton("YES") { _, _ ->
            FirebaseAuth.getInstance().signOut()
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.putExtra("EXIT", true)
            startActivity(i)
        }
        builder.setNegativeButton("NO") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }


    private fun nomineeDialogBox(){
        val builder = AlertDialog.Builder(context)
        val inflater = layoutInflater
        builder.setTitle("Add Nominee")
        val dialogLayout = inflater.inflate(R.layout.addnomineelayout, null)
        val editTextNumber  = dialogLayout.findViewById<EditText>(R.id.ediText_number)
        val editTextName  = dialogLayout.findViewById<EditText>(R.id.ediText_nominee_name)
        builder.setView(dialogLayout)
        builder.setPositiveButton("Confirm") { _, _ ->
            if(editTextNumber.text.isNullOrBlank()||editTextNumber.text.length!=10||editTextName.text.isNullOrBlank()){
                Toast.makeText(context, "invalid fields", Toast.LENGTH_SHORT).show()
            }
            else{
                var isMemberExist=false
                customerObject!!.nominee.values.forEach {
                    if(it.toLong()==(editTextNumber.text.toString()).toLong()){
                        Toast.makeText(context, "Nominee Exist", Toast.LENGTH_SHORT).show()
                        isMemberExist=true
                    }
                }
                if (!isMemberExist){
                    customerObject!!.nominee[editTextName.text.toString()] = (editTextNumber.text.toString()).toLong()
                    FirebaseFunctions.getInstance().getHttpsCallable("UpdateCustomer").call(
                        mapOf(
                            "userID" to user!!.uid,
                            "key" to "nominee",
                            "value" to customerObject!!.nominee
                        )
                    ).addOnCompleteListener { isChanged ->
                        when (!isChanged.isSuccessful) {
                            true -> Toast.makeText(context, "update failed", Toast.LENGTH_SHORT)
                                .show()
                            else-> Toast.makeText(context, "uploaded", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        builder.show()
    }

    private fun editingDialogBox(){
        val builder = AlertDialog.Builder(context)
        val inflater = layoutInflater
        builder.setTitle("Change Username")
        val dialogLayout = inflater.inflate(R.layout.dialogwithedittext, null)
        val editText  = dialogLayout.findViewById<EditText>(R.id.ediText_number)
        editText.hint="enter your name"
        editText.inputType=InputType.TYPE_CLASS_TEXT
        builder.setView(dialogLayout)
        builder.setPositiveButton("Confirm") { _, _ ->
            if(editText.text.isNullOrBlank()){
                Toast.makeText(context, "invalid fields", Toast.LENGTH_SHORT).show()
            }else{
                FirebaseFunctions.getInstance().getHttpsCallable("UpdateCustomer").call(
                    mapOf(
                        "userID" to user!!.uid,
                        "key" to "username",
                        "value" to editText.text.toString()
                    )
                ).addOnCompleteListener { isChanged ->
                    when (!isChanged.isSuccessful) {
                        true -> Toast.makeText(context, "update failed", Toast.LENGTH_SHORT).show()
                        else-> {
                            val capitalized = StringBuilder(editText.text.toString())
                            capitalized.setCharAt(0, Character.toUpperCase(capitalized[0]))
                            customerObject!!.username = "$capitalized"
                            profileUserName.text = "$capitalized "
                            Toast.makeText(context, "uploaded", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        builder.show()
    }

    private fun setData(){
        if(customerObject!!.imageUrl.compareTo("no image")==0){ profileImage.setImageDrawable(
            resources.getDrawable(R.drawable.profieicon)
        ) }
        else{ Picasso.get().load(customerObject!!.imageUrl).into(profileImage) }
        val capitalized = StringBuilder(customerObject!!.username)
        capitalized.setCharAt(0, Character.toUpperCase(capitalized[0]))
        profileUserName.text= "$capitalized "
        profilePhoneNumber.text= "${customerObject!!.phone} "
        profileImageProgress.hide()
    }

    private fun checkPermissionAndPickImage(){
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE
                ) }
            else{
                Toast.makeText(context, "permission denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK &&requestCode == REQUEST_CODE&&data?.data!=null) {
            profileImage.setImageURI(data.data)
            profileImageProgress.show()
            val bitMapData = (profileImage.drawable as BitmapDrawable).bitmap
            val ref = storageReference.child("AutoCustomer").child("ProfilePics/" + user?.uid + ".jpg")
            val stream= ByteArrayOutputStream()
            bitMapData.compress(Bitmap.CompressFormat.JPEG, 20, stream)
            val picData = stream.toByteArray()
            val uploadTask: UploadTask = ref.putBytes(picData)
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                ref.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    FirebaseFunctions.getInstance().getHttpsCallable("UpdateCustomer").call(
                        mapOf(
                            "userID" to user!!.uid,
                            "key" to "imageUrl",
                            "value" to downloadUri.toString()
                        )
                    ).addOnCompleteListener { isChanged ->
                        when (!isChanged.isSuccessful) {
                            true -> {
                                profileImageProgress.hide()
                                Toast.makeText(
                                    context,
                                    "upload profile pic failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (customerObject!!.imageUrl.compareTo("no image") == 0) {
                                    profileImage.setImageDrawable(resources.getDrawable(R.drawable.profieicon))
                                } else {
                                    Picasso.get().load(customerObject!!.imageUrl).into(profileImage)
                                }
                            }
                            else-> {
                                profileImageProgress.hide()
                                customerObject!!.imageUrl=downloadUri.toString()
                                Toast.makeText(context, "uploaded", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    profileImageProgress.hide()
                    Toast.makeText(context, "upload profile pic failed", Toast.LENGTH_SHORT).show()
                    if(customerObject!!.imageUrl.compareTo("no image")==0){
                        profileImage.setImageDrawable(resources.getDrawable(R.drawable.profieicon))
                    }
                    else{
                        Picasso.get().load(customerObject!!.imageUrl).into(profileImage)
                    }
                }
            }
        }
    }
}