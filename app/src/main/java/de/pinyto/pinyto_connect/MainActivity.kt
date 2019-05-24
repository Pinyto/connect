package de.pinyto.pinyto_connect

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.widget.TextView

class MainActivity: AbstractPinytoServiceConnectedActivity() {
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

    @SuppressLint("HandlerLeak")
    inner class MessagesFromPinytoServiceHandler: Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg == null) return
            val data = msg.data
            if (!data.containsKey("tag")) {
                Log.e(this@MainActivity.localClassName, "Answering messages to the need to have a tag!")
                return
            }
            when (data.getString("tag")) {
                "checkPinyto" -> {
                    Log.d(this@MainActivity.localClassName, "checkPinyto received.")
                    if (!data.containsKey("pinytoReady")) {
                        Log.e(this@MainActivity.localClassName, "The answer contains no key \"pinytoReady\".")
                        return
                    }
                    Log.i("checkPinytoAnswer", data.getBoolean("pinytoReady").toString())
                    if (!data.getBoolean("pinytoReady")) {
                        val loginRegisterIntent = Intent(
                            this@MainActivity,
                            KeyserverLoginRegisterActivity::class.java
                        )
                        startActivity(loginRegisterIntent)
                    }
                }
            }
        }
    }

    private val pinytoServiceReturnMessenger = Messenger(MessagesFromPinytoServiceHandler())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        textMessage = findViewById(R.id.message)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
    }

    override fun onPinytoServiceConnected() {
        checkPinyto()
    }

    private fun createAnswerBundle(tag: String): Bundle {
        val bundle = Bundle()
        bundle.putString("tag", tag)
        bundle.putBinder("answerBinder", pinytoServiceReturnMessenger.binder)
        return bundle
    }

    private fun checkPinyto() {
        Log.d(this.localClassName, "checkPinyto().")
        Log.i(this.localClassName, "pinytoServiceIsBound: $pinytoServiceIsBound")
        if (!pinytoServiceIsBound) return
        val msg = Message.obtain()
        val bundle = createAnswerBundle("checkPinyto")
        bundle.putString("path", "#check_pinyto")
        msg.data = bundle
        try {
            pinytoServiceMessenger?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        Log.d(this.localClassName, "checkPinyto sent.")
    }

    fun buttonClick(view: View) {
        //val connector = PinytoConnector()
        //connector.getTokenFromKeyserver("pina", "a12b3")
        Log.d("pinytoServiceIsBound", pinytoServiceIsBound.toString())

        if (!pinytoServiceIsBound) return
        val msg = Message.obtain()
        val bundle = Bundle()
        //bundle.putString("toastedMessage", "It worked!")
        bundle.putString("path", "/foo")
        msg.data = bundle
        try {
            pinytoServiceMessenger?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}
