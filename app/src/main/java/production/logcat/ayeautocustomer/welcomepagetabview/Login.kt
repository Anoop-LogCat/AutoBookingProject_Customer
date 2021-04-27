package production.logcat.ayeautocustomer.welcomepagetabview

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import production.logcat.ayeautocustomer.*
import production.logcat.ayeautocustomer.models.phoneLogin

class Login : Fragment() {

    private lateinit var loginPhone:EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginPhone=view.findViewById(R.id.login_phone)
        if(phoneLogin.compareTo("phone")!=0){
            loginPhone.setText(phoneLogin)
        }
        loginPhone.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(mEdit: Editable) {
                phoneLogin = mEdit.toString()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }
}