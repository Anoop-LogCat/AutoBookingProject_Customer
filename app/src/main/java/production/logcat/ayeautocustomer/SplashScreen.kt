package production.logcat.ayeautocustomer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.jsoup.Jsoup
import production.logcat.ayeautocustomer.models.FireCustomerModel
import production.logcat.ayeautocustomer.models.customerObject
import production.logcat.ayeautocustomer.models.dialogAlertBox
import java.io.IOException


class SplashScreen : Fragment() {

    private val user = FirebaseAuth.getInstance().currentUser
    private var navController:NavController?=null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splash_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        val packageInfo: PackageInfo? = try {
            requireContext().packageManager?.getPackageInfo(requireContext().packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        if(packageInfo!=null){
            val currentVersion = packageInfo.versionName
            ForceUpdateAsync(currentVersion, requireContext()).execute()
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ForceUpdateAsync(private val currentVersion: String, private val context: Context) : AsyncTask<String?, String?, JSONObject>() {
        private var latestVersion: String? = null

        override fun doInBackground(vararg params: String?): JSONObject {
            try {
                latestVersion = Jsoup.connect("https://play.google.com/store/apps/details?id=" + context.packageName.toString() + "&hl=en")
                        .timeout(30000)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .get()
                        .select("div[itemprop=softwareVersion]")
                        .first()
                        .ownText()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return JSONObject()
        }

        override fun onPostExecute(jsonObject: JSONObject) {
            super.onPostExecute(jsonObject)
            if (latestVersion != null) {
                if (!currentVersion.equals(latestVersion, ignoreCase = true)) {
                    dialogAlertBox(resources.getDrawable(R.drawable.tagdialogbackgroundstyle), requireContext(), "Update Required", "There is new version available on Play Store please update the app", "Update Now", "", R.drawable.refresh, okFunc = {
                        requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.packageName)))
                    }, cancelFunc = {}, okVisible = true, cancelVisible = false)
                }
                else{
                    if(user!=null){
                        GetUserData().execute()
                    }
                    else{
                        navController!!.navigate(R.id.action_splashScreen_to_welcomeScreen)
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    private inner class GetUserData : AsyncTask<String, Void, String>(){
        override fun doInBackground(vararg params: String?): String {
            return try {
                val url = "https://us-central1-auto-pickup-apps.cloudfunctions.net/ViewCustomer/${user?.uid}"
                val request: okhttp3.Request = okhttp3.Request.Builder().url(url).build()
                val response= OkHttpClient().newCall(request).execute()
                response.body()?.string().toString()
            }catch (e: Exception){
                "ERROR"
            }
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(result!!.compareTo("ERROR")==0){
                dialogAlertBox(resources.getDrawable(R.drawable.tagdialogbackgroundstyle), requireContext(), "No Connectivity", "Please turn on the internet and press OK", "OK", "", R.drawable.smartphone, okFunc = {
                    GetUserData().execute()
                }, cancelFunc = {}, okVisible = true, cancelVisible = false)
            }
            else{
                customerObject = Gson().fromJson(result, FireCustomerModel::class.java)
                navController!!.navigate(R.id.action_splashScreen_to_home2)
            }
        }
    }
}