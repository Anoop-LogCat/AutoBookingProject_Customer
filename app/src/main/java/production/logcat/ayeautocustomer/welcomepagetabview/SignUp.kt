package production.logcat.ayeautocustomer.welcomepagetabview

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import production.logcat.ayeautocustomer.*
import production.logcat.ayeautocustomer.models.phoneSignUp
import production.logcat.ayeautocustomer.models.usernameSignUp

class SignUp : Fragment() {

    private lateinit var signUpUsername: EditText
    private lateinit var signUpPhone: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signUpUsername=view.findViewById(R.id.signUpUserName)
        signUpPhone=view.findViewById(R.id.signUpPhone)
        if(usernameSignUp.compareTo("username")!=0&& phoneSignUp.compareTo("phone")!=0){
            signUpUsername.setText(usernameSignUp)
            signUpPhone.setText(phoneSignUp)
        }
        signUpUsername.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(mEdit: Editable) {
                usernameSignUp = mEdit.toString()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
        signUpPhone.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(mEdit: Editable) {
                phoneSignUp = mEdit.toString()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }
}