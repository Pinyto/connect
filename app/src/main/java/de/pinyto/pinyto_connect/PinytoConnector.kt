package de.pinyto.pinyto_connect

import android.util.Log
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URL

class PinytoConnector() {
    fun authenticate(username: String, password: String) {
        doAsync {
            val result = URL("${prefs.pinytoUrl}/authenticate").readText()
            uiThread {
                Log.d("Request", result)
            }
        }
    }
}
