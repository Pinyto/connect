package de.pinyto.pinyto_connect

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import org.jetbrains.anko.doAsync
import org.json.JSONObject

const val CHECK_PINYTO = "checkPinyto"
const val REGISTER_KEY = "registerKey"

class PinytoService: Service() {
    private lateinit var pinytoKeyManager: PinytoKeyManager
    private lateinit var pinytoConnector: PinytoConnector

    @SuppressLint("HandlerLeak")
    inner class IncomingHandler: Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg == null) return
            val data = msg.data

            if (!data.containsKey("path")) {
                Log.e("PinytoService", "Messages to the bound service need to have a path!")
                return
            }
            Log.i("PinytoService", "handleMessage() with path: " + data.getString("path"))
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
            when (data.getString("path")) {
                "#check_pinyto" -> {
                    pinytoKeyManager.loadKeyFromPrefs()
                    Log.i("PinytoService", "Key exisits: ${pinytoKeyManager.keyExists()}\nKey is registered: ${prefs.savedKeyIsRegistered}")
                    sendAnswer(data.getString("tag"), data.getBinder("answerBinder")) {
                        it.putBoolean("pinytoReady", pinytoKeyManager.keyExists() && prefs.savedKeyIsRegistered)
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
                    ) { token -> registerKey(token, data.getString("username", ""),
                        data.getString("tag"), data.getBinder("answerBinder")) }
                }
                "/keyserver/register" -> {
                    if (!data.containsKey("username") || !data.containsKey("password")) {
                        Log.e(
                            "PinytoService",
                            "Registration at the keyserver is only possible with a username and a password."
                        )
                        return
                    }
                    val username = data.getString("username", "")
                    val password = data.getString("password", "")
                    Log.i("PinytoService", "Registering at keyserver with $username and $password")
                    pinytoConnector.registerAtKeyserver(username, password, fun (success: Boolean) {
                        Log.i("PinytoService", "Callback for register: $success")
                        if (success) {
                            pinytoConnector.getTokenFromKeyserver(username, password)
                            { token -> registerKey(token, data.getString("username", ""),
                                data.getString("tag"), data.getBinder("answerBinder")) }
                        }
                    })
                }
                "/authenticate" -> {

                }
                else -> {
                    val payload = JSONObject(data.getString("payload", "{}"))
                    pinytoConnector.assemblyRequest(data.getString("path", "/"), payload,
                        fun (answer: String) {
                        sendAnswer(data.getString("tag"), data.getBinder("answerBinder")) {
                            it.putString("answer", answer)
                        }
                    })
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

    private fun sendAnswer(answerTag: String?, answerBinder: IBinder?, bundleExtender: (answerBundle: Bundle) -> Unit) {
        val answerMessenger = Messenger(answerBinder)
        val answer = Message.obtain()
        val answerBundle = Bundle()
        answerBundle.putString("tag", answerTag)
        bundleExtender(answerBundle)
        answer.data = answerBundle
        try {
            answerMessenger.send(answer)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun registerKey(keyserverToken: String, username: String, answerTag: String?, answerBinder: IBinder?) {
        pinytoKeyManager.loadKeyFromPrefs()
        Log.i("PinytoService", "KeyManager: ${pinytoKeyManager.getPublicKeyData().toString(0)}")
        if (!pinytoKeyManager.keyExists()) {
            Log.e("PinytoService", "Could not register key because the key manager did not have one.")
            return
        }
        pinytoConnector.registerKey(keyserverToken, pinytoKeyManager) {
            success -> prefs.savedKeyIsRegistered = success
            if (success) prefs.username = username
            pinytoKeyManager.saveKeyToPrefs()
            if (answerTag != null && answerBinder != null) {
                sendAnswer(answerTag, answerBinder) {
                    it.putBoolean("registeredKey", success)
                }
            }
        }
    }
}