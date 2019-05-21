package de.pinyto.pinyto_connect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.content.ServiceConnection
import android.os.*
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    var pinytoServiceMessenger: Messenger? = null
    var pinytoServiceIsBound: Boolean = false

    private lateinit var textMessage: TextView
    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                textMessage.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                textMessage.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                textMessage.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }
    private val pinytoServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            pinytoServiceMessenger = Messenger(service)
            pinytoServiceIsBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            pinytoServiceMessenger = null
            pinytoServiceIsBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        textMessage = findViewById(R.id.message)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        val bindPinytoServiceIntent = Intent(applicationContext, PinytoService::class.java)
        bindService(bindPinytoServiceIntent, pinytoServiceConnection, Context.BIND_AUTO_CREATE)
    }

    fun buttonClick(view: View) {
        //val connector = PinytoConnector()
        //connector.getTokenFromKeyserver("pina", "a12b3")

        if (!pinytoServiceIsBound) return
        val msg = Message.obtain()
        val bundle = Bundle()
        bundle.putString("toastedMessage", "It worked!")
        msg.data = bundle
        try {
            pinytoServiceMessenger?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}
