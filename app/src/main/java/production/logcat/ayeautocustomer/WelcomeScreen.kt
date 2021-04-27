package production.logcat.ayeautocustomer

import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import production.logcat.ayeautocustomer.adapters.WelcomePageAdapter
import production.logcat.ayeautocustomer.models.phoneLogin
import production.logcat.ayeautocustomer.models.phoneSignUp
import production.logcat.ayeautocustomer.models.usernameSignUp
import java.util.concurrent.TimeUnit

class WelcomeScreen : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private val auth:FirebaseAuth= FirebaseAuth.getInstance()

    private lateinit var confirm: RelativeLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_welcome_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabLayout=view.findViewById(R.id.welcomePageTabs)
        viewPager=view.findViewById(R.id.welcomePageViewPagers)
        confirm=view.findViewById(R.id.auth_confirm_button)
        viewPager.adapter= WelcomePageAdapter(this)
        TabLayoutMediator(tabLayout,viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Sign Up"
                1 -> tab.text = "Log In"
            }
        }.attach()
        confirm.setOnClickListener {
            if(viewPager.currentItem==0){
                if(phoneSignUp.isEmpty() || phoneSignUp.compareTo("phone")==0 || phoneSignUp.length!=10 || usernameSignUp.isEmpty() || usernameSignUp.compareTo("username")==0){
                    Toast.makeText(requireActivity(),"Invalid Fields",Toast.LENGTH_SHORT).show()
                }
                else{
                    verifyPhoneNumber(view, usernameSignUp, phoneSignUp,"SIGN_UP")
                }
            }
            else if(viewPager.currentItem==1){
                if(phoneLogin.isEmpty() || phoneLogin.compareTo("phone")==0 || phoneLogin.length!=10){
                    Toast.makeText(requireActivity(),"Invalid Fields",Toast.LENGTH_SHORT).show()
                }
                else{
                    verifyPhoneNumber(view,"no username", phoneLogin,"LOG_IN")
                }
            }
        }
    }
    private fun verifyPhoneNumber(viewOfPage:View,username:String,phoneNumber:String,authType:String){
        val progressDialog=ProgressDialog(requireActivity())
        progressDialog.setMessage("Verifying the Phone Number")
        progressDialog.setCancelable(false)
        progressDialog.show()
        val callbacks = object :PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(p0: PhoneAuthCredential) { }
            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.cancel()
                if (e is FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(requireActivity(),"Invalid Request",Toast.LENGTH_SHORT).show()
                } else if (e is FirebaseTooManyRequestsException) {
                    Toast.makeText(requireActivity(),"SMS Quota has been expired",Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                progressDialog.cancel()
                val action=WelcomeScreenDirections.actionWelcomeScreenToVerficationScreen()
                action.authType=authType
                action.username=username
                action.phoneNumber=phoneNumber
                action.verificationCode=verificationId
                val navController:NavController=Navigation.findNavController(viewOfPage)
                navController.navigate(action)
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+91$phoneNumber")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(callbacks)
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}