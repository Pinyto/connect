package de.pinyto.pinyto_connect

import android.util.Log
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class PinytoConnector() {
    private fun jsonPostRequest(path: String, requestPayload: JSONObject) {
        doAsync {
            val postData = requestPayload.toString().toByteArray(StandardCharsets.UTF_8)
            val connection = URL(prefs.pinytoUrl + path).openConnection() as HttpsURLConnection
            connection.connectTimeout = 300000
            connection.connectTimeout = 300000
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("charset", "utf-8")
            connection.setRequestProperty("Content-length", postData.size.toString())
            try {
                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.write(postData)
                outputStream.flush()
            } catch (exception: Exception) {
                Log.e("PinytoConnector", exception.message)
            }
            val inputStreamReader = BufferedReader(InputStreamReader(connection.inputStream))
            val contentBuilder = StringBuilder()
            var currentLine = inputStreamReader.readLine()
            while (currentLine != null) {
                contentBuilder.append(currentLine).append("\n")
                currentLine = inputStreamReader.readLine()
            }
            val result = contentBuilder.toString()
            val responseJson = JSONObject(result)
            uiThread {
                Log.d("Request", connection.responseCode.toString())
                Log.d("Request", result)
                Log.d("Request", responseJson.toString())
            }
        }
    }

    fun authenticate(username: String, pkm: PinytoKeyManager) {
        val requestPayload = JSONObject()
        requestPayload.put("username", username)
        requestPayload.put("key_hash", pkm.getKeyHash())
        jsonPostRequest("/authenticate", requestPayload)
    }

    fun getTokenFromKeyserver(username: String, password: String) {
        val requestPayload = JSONObject()
        requestPayload.put("name", username)
        requestPayload.put("password", password)
        jsonPostRequest("/keyserver/authenticate", requestPayload)
    }

    fun registerKey(token: String, pkm: PinytoKeyManager) {
        val requestPayload = JSONObject()
        requestPayload.put("token", token)
        requestPayload.put("public_key", pkm.getPublicKeyData())
        jsonPostRequest("/register_new_key", requestPayload)
    }

    fun logout(token: String) {
        val requestPayload = JSONObject()
        requestPayload.put("token", token)
        jsonPostRequest("/logout", requestPayload)
    }
}
