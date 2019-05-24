package de.pinyto.pinyto_connect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Messenger
import androidx.appcompat.app.AppCompatActivity

abstract class AbstractPinytoServiceConnectedActivity: AppCompatActivity() {
    var pinytoServiceMessenger: Messenger? = null
    var pinytoServiceIsBound: Boolean = false

    private val pinytoServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            pinytoServiceMessenger = Messenger(service)
            pinytoServiceIsBound = true
            onPinytoServiceConnected()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            pinytoServiceMessenger = null
            pinytoServiceIsBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindPinytoServiceIntent = Intent("de.pinyto.pinyto_connect.BIND")
        bindPinytoServiceIntent.setPackage("de.pinyto.pinyto_connect")
        applicationContext.bindService(bindPinytoServiceIntent, pinytoServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        //if (pinytoServiceIsBound) unbindService(pinytoServiceConnection)
        super.onDestroy()
    }

    open fun onPinytoServiceConnected() {}
}
