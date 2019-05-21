package de.pinyto.pinyto_connect

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.*
import android.widget.Toast

class PinytoService: Service() {
    @SuppressLint("HandlerLeak")
    inner class IncomingHandler: Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg == null) return
            val data = msg.data
            val dataString = data.getString("toastedMessage")
            Toast.makeText(applicationContext, dataString, Toast.LENGTH_SHORT).show()
        }
    }

    private val bindingMessenger = Messenger(IncomingHandler())

    override fun onBind(intent: Intent?): IBinder? {
        return bindingMessenger.binder
    }
}