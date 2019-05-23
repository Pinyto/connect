package de.pinyto.pinyto_connect

import android.os.Bundle
import android.view.View
import android.widget.Button
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_keyserver_login_register.*

class KeyserverLoginRegisterActivity : AppCompatActivity() {
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

    }
}
