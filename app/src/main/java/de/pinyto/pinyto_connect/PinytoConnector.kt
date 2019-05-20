package de.pinyto.pinyto_connect

import android.util.Log
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.StringBuilder
import java.net.URL

class PinytoConnector() {
    fun authenticate(username: String, keyHash: String) {
        doAsync {
            val connection = URL("${prefs.pinytoUrl}/authenticate").openConnection()
            connection.doInput = true
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.connect()
            val requestPayload = JSONObject()
            requestPayload.put("username", username)
            requestPayload.put("key_hash", keyHash)
            val outputStreamWriter = OutputStreamWriter(connection.getOutputStream())
            outputStreamWriter.write(requestPayload.toString())
            outputStreamWriter.flush()
            val inputStreamReader = BufferedReader(InputStreamReader(connection.getInputStream()))
            val contentBuilder = StringBuilder()
            var currentLine = inputStreamReader.readLine()
            while (currentLine != null) {
                contentBuilder.append(currentLine).append("\n")
                currentLine = inputStreamReader.readLine()
            }
            val result = contentBuilder.toString()
            uiThread {
                Log.d("Request", result)
            }
        }
    }
}
