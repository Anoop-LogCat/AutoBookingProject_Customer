package production.logcat.ayeautocustomer

import `in`.aabhasjindal.otptextview.OtpTextView
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.OkHttpClient
import okhttp3.Request

class VerificationScreen : Fragment() {

    private lateinit var closeButton:ImageButton
    private lateinit var progressDialogInVerification:ProgressDialog
    private var navController:NavController?=null
    private var user:FirebaseUser?=null

    private val argsInVerificationScreen:VerificationScreenArgs by navArgs()
    private val auth:FirebaseAuth= FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_verfication_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController=Navigation.findNavController(view)
        val otpEditText:OtpTextView=view.findViewById(R.id.otpEditTextForVerify)
        closeButton=view.findViewById(R.id.closeForVerify)
        progressDialogInVerification=ProgressDialog(requireActivity())
        closeButton.setOnClickListener {
            navController!!.navigate(R.id.action_verficationScreen_to_welcomeScreen)
        }
        view.findViewById<Button>(R.id.verifyButton).setOnClickListener {
            if(otpEditText.otp?.length==6){
                progressDialogInVerification.setMessage("Verifying the Phone Number")
                progressDialogInVerification.setCancelable(false)
                progressDialogInVerification.show()
                val credential = PhoneAuthProvider.getCredential(argsInVerificationScreen.verificationCode, otpEditText.otp!!)
                auth.signInWithCredential(credential).addOnCompleteListener(requireActivity()){task->
                    if (task.isSuccessful) {
                        user=task.result!!.user
                        if(argsInVerificationScreen.authType.compareTo("LOG_IN")==0){
                            CheckTheUser().execute(argsInVerificationScreen.phoneNumber)
                        }
                        else if(argsInVerificationScreen.authType.compareTo("SIGN_UP")==0){
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task2 ->
                                if (task2.isSuccessful) {
                                    val userData= mapOf<String,Any>(
                                        "username" to argsInVerificationScreen.username,
                                        "phone" to argsInVerificationScreen.phoneNumber,
                                        "imageUrl" to "no image",
                                        "token" to task2.result.toString(),
                                        "history" to mapOf(
                                            System.currentTimeMillis().toString() to "demoJSONString",
                                        ),
                                        "nominee" to mapOf<String,Long>(
                                            "0" to 1234567890
                                        )
                                    )
                                    FirebaseFunctions.getInstance().getHttpsCallable("SaveCustomer").call(userData).addOnCompleteListener { task3->
                                        if (task3.isSuccessful) {
                                            progressDialogInVerification.cancel()
                                            val i = Intent(context, MainActivity::class.java)
                                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            i.putExtra("EXIT", true)
                                            startActivity(i)
                                        }
                                        else {
                                            progressDialogInVerification.cancel()
                                            task.result?.user?.delete()?.addOnCompleteListener {
                                                Toast.makeText(context,"failed to sign Up", Toast.LENGTH_SHORT).show()
                                                navController!!.navigate(R.id.action_verficationScreen_to_welcomeScreen)
                                            }
                                        }
                                    }
                                }
                                else{
                                    progressDialogInVerification.cancel()
                                    task.result?.user?.delete()?.addOnCompleteListener {
                                        Toast.makeText(context,"failed to sign Up", Toast.LENGTH_SHORT).show()
                                        navController!!.navigate(R.id.action_verficationScreen_to_welcomeScreen)
                                    }
                                }
                            }
                        }
                    } else {
                        progressDialogInVerification.cancel()
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(requireActivity(),"otp incorrect", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            else{
                Toast.makeText(requireActivity(),"otp invalid", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    private inner class CheckTheUser : AsyncTask<String, Void, String>(){

        override fun doInBackground(vararg params: String?): String {
            val url = "https://us-central1-auto-pickup-apps.cloudfunctions.net/CheckCustomerExist/${params[0]!!}"
            val request: Request = Request.Builder().url(url).build()
            val response= OkHttpClient().newCall(request).execute()
            return response.body()?.string().toString()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(result!!.compareTo("USER EXIST")==0){
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task2 ->
                    if (task2.isSuccessful) {
                        FirebaseFunctions.getInstance().getHttpsCallable("UpdateCustomer").call(mapOf("userID" to user!!.uid,"key" to "token", "value" to task2.result.toString())).addOnCompleteListener { isChanged ->
                            when (isChanged.isSuccessful) {
                                true -> {
                                    progressDialogInVerification.cancel()
                                    val i = Intent(context, MainActivity::class.java)
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    i.putExtra("EXIT", true)
                                    startActivity(i)
                                }
                                else-> {
                                    progressDialogInVerification.cancel()
                                    Toast.makeText(context, "failed to login", Toast.LENGTH_SHORT).show()
                                    FirebaseAuth.getInstance().signOut()
                                    navController!!.navigate(R.id.action_verficationScreen_to_welcomeScreen)
                                }
                            }
                        }
                    }
                    else{
                        progressDialogInVerification.cancel()
                        Toast.makeText(context, "failed to login", Toast.LENGTH_SHORT).show()
                        FirebaseAuth.getInstance().signOut()
                        navController!!.navigate(R.id.action_verficationScreen_to_welcomeScreen)
                    }
                }
            }
            else{
                progressDialogInVerification.cancel()
                Toast.makeText(context, "no account",Toast.LENGTH_SHORT).show()
                auth.currentUser?.delete()!!.addOnCompleteListener {
                    navController!!.navigate(R.id.action_verficationScreen_to_welcomeScreen)
                }
            }
        }
    }
}