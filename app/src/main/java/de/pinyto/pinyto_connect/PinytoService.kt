package de.pinyto.pinyto_connect

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import org.jetbrains.anko.doAsync

class PinytoService: Service() {
    private lateinit var pinytoKeyManager: PinytoKeyManager
    private lateinit var pinytoConnector: PinytoConnector

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler: Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg == null) return
            val data = msg.data
            //val dataString = data.getString("toastedMessage")
            //Toast.makeText(applicationContext, dataString, Toast.LENGTH_SHORT).show()

            if (!data.containsKey("path")) {
                Log.e("PinytoService", "Messages to the bound service need to have a path!")
                return
            }
            Log.d("PinytoService", "handleMessage() with path: " + data.getString("path"))
            when (data.getString("path")) {
                "#check_pinyto" -> {
                    if (!data.containsKey("answerBinder")) {
                        Log.e(
                            "PinytoService",
                            "Checking is useless without an answer IBinder to send the result."
                        )
                        return
                    }
                    if (!data.containsKey("tag")) {
                        Log.e(
                            "PinytoService",
                            "The request needs a tag so the answer can have one and is traceable by it."
                        )
                        return
                    }
                    val answerMessenger = Messenger(data.getBinder("answerBinder"))
                    val answer = Message.obtain()
                    val answerBundle = Bundle()
                    answerBundle.putString("tag", data.getString("tag"))
                    answerBundle.putBoolean("pinytoReady", pinytoKeyManager.keyExists() && prefs.savedKeyIsRegistered)
                    answer.data = answerBundle
                    try {
                        answerMessenger.send(answer)
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
                "/keyserver/authenticate" -> {
                    if (!data.containsKey("username") || !data.containsKey("password")) {
                        Log.e(
                            "PinytoService",
                            "Authentication at the keyserver is only possible with a username and a password."
                        )
                        return
                    }
                    pinytoConnector.getTokenFromKeyserver(
                        data.getString("username", ""),
                        data.getString("password", "")
                    ) { token -> processTokenAndRegisterKey(token) }
                }
            }
        }
    }

    private val bindingMessenger = Messenger(IncomingHandler())

    override fun onCreate() {
        super.onCreate()
        pinytoKeyManager = PinytoKeyManager()
        pinytoKeyManager.loadKeyFromPrefs()
        if (!pinytoKeyManager.keyExists()) {
            doAsync {
                pinytoKeyManager.generateNewKeys()
            }
        }
        pinytoConnector = PinytoConnector()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return bindingMessenger.binder
    }

    fun processTokenAndRegisterKey(encryptedToken: String) {
        if (!pinytoKeyManager.keyExists()) {
            Log.e("PinytoService", "Could not register key because the key manager did not have one.")
            return
        }
        val keyserverToken = pinytoKeyManager.calculateToken(encryptedToken)
        pinytoConnector.registerKey(keyserverToken, pinytoKeyManager) {
            success -> prefs.savedKeyIsRegistered = success
        }
    }
}