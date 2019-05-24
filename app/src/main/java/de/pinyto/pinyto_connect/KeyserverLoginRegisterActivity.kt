package de.pinyto.pinyto_connect

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_keyserver_login_register.*

class KeyserverLoginRegisterActivity: AbstractPinytoServiceConnectedActivity() {
    private lateinit var usernameTextView: TextView
    private lateinit var password1TextView: TextView
    private lateinit var password2TextView: TextView
    private lateinit var button: Button

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.keyserver_navigation_login -> {
                password2TextView.visibility = View.INVISIBLE
                button.text = getString(R.string.login)
                return@OnNavigationItemSelectedListener true
            }
            R.id.keyserver_navigation_register -> {
                password2TextView.visibility = View.VISIBLE
                button.text = getString(R.string.register)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyserver_login_register)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        usernameTextView = findViewById(R.id.usernameEdit)
        password1TextView = findViewById(R.id.passwordEdit1)
        password2TextView = findViewById(R.id.passwordEdit2)
        button = findViewById(R.id.loginRegisterButton)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
    }

    fun loginOrRegister(view: View) {
        val msg = Message.obtain()
        val bundle = Bundle()
        if ( nav_view.selectedItemId == R.id.keyserver_navigation_register ) {
            if (!password1TextView.text.toString().contentEquals(password2TextView.text)) {
                Toast.makeText(this, R.string.password_mismatch, Toast.LENGTH_SHORT).show()
                return
            }
            bundle.putString("path", "/keyserver/register")
        } else {
            bundle.putString("path", "/keyserver/authenticate")
        }
        bundle.putString("username", usernameTextView.text.toString())
        bundle.putString("password", password1TextView.text.toString())
        msg.data = bundle
        try {
            pinytoServiceMessenger?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}
