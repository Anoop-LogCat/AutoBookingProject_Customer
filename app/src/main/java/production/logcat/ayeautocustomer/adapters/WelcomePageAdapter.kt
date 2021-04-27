package production.logcat.ayeautocustomer.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import production.logcat.ayeautocustomer.WelcomeScreen
import production.logcat.ayeautocustomer.welcomepagetabview.Login
import production.logcat.ayeautocustomer.welcomepagetabview.SignUp

class WelcomePageAdapter (fragmentActivity: WelcomeScreen) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 2
    }
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0-> SignUp()
            else-> Login()
        }
    }
}