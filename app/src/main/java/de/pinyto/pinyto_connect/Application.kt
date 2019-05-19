package de.pinyto.pinyto_connect

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class SharedPreferencesWrapper(context: Context) {
    private val prefsFilename = "de.pinyto.pinyto_connect.prefs"
    private val jsonKeyPairKey = "private_key_json"
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsFilename, 0)

    var jsonKeyPair: JSONObject
        get() = JSONObject(prefs.getString(jsonKeyPairKey, "{}"))
        set(value) = prefs.edit().putString(jsonKeyPairKey, value.toString()).apply()
}

val prefs: SharedPreferencesWrapper = PreferencedApplication.sharedPreferences!!

class PreferencedApplication: Application() {
    companion object {
        var sharedPreferences: SharedPreferencesWrapper? = null
    }

    override fun onCreate() {
        sharedPreferences = SharedPreferencesWrapper(applicationContext)
        super.onCreate()
    }
}