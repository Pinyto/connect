package de.pinyto.pinyto_connect;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * This class handles all requests to the Pinyto cloud.
 */
public class PinytoRequest extends AsyncTask<String, Void, String> {

    public static String pinytoHost = "www.pinyto.de";

    final Context contentContext;

    private String token = null;

    public PinytoRequest(Context contentContext) {
        this.contentContext = contentContext;
    }

    private KeyStore buildKeystore() throws
            KeyStoreException,
            CertificateException,
            NoSuchAlgorithmException,
            IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = contentContext.getResources().openRawResource(R.raw.pinyto);
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
        } finally {
            caInput.close();
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);
        return keyStore;
    }

    private TrustManager[] getTrustManagers() throws
            NoSuchAlgorithmException,
            KeyStoreException,
            CertificateException,
            IOException {
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(buildKeystore());
        return tmf.getTrustManagers();
    }

    private SSLContext getSSLContext() throws
            NoSuchAlgorithmException,
            KeyManagementException,
            KeyStoreException,
            CertificateException,
            IOException {
        SSLContext sslcontext = SSLContext.getInstance("TLS");
		sslcontext.init(null, getTrustManagers(), null);
		return sslcontext;
    }

    private HttpsURLConnection establishHttpsConnection(String path) {
        try {
            URL url = new URL("https://" + pinytoHost + "/" + path);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(getSSLContext().getSocketFactory());
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Host", pinytoHost);
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            return connection;
        } catch (MalformedURLException e) {
            Log.d("Authentication error", "The URL is malformed. Probably pinytoUrl is wrong.");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            Log.d("Authentication error", "Could not establish connection.");
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.d("Authentication error", "TLS or TrustManager is not implemented.");
            e.printStackTrace();
            return null;
        } catch (KeyManagementException e) {
            Log.d("Authentication error", "Key management is broken.");
            e.printStackTrace();
            return null;
        } catch (KeyStoreException e) {
            Log.d("Authentication error", "KeyStore is broken.");
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            Log.d("Authentication error", "The local certificate is invalid.");
            e.printStackTrace();
            return null;
        }
    }

    private void authenticate(String username, PinytoKeyManager pkm) {
        HttpsURLConnection connection = establishHttpsConnection("authenticate");
        if (connection != null) {
            try {
                DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());
                JSONObject requestData = new JSONObject();
                requestData.put("username", username);
                requestData.put("key_hash", pkm.getKeyHash());
                requestStream.writeBytes(requestData.toString());
                requestStream.flush();
                requestStream.close();
                InputStream responseStream = connection.getInputStream();
                InputStreamReader responseReader = new InputStreamReader(responseStream);
                BufferedReader bufferedResponseReader = new BufferedReader(responseReader);
                String response = "";
                String responseLine;
                while ((responseLine = bufferedResponseReader.readLine()) != null) {
                    response += responseLine;
                }
                Log.d("authenticate response", response);
                JSONObject responseObject = new JSONObject(response);
                //Log.d("signature verification", Boolean.toString(pkm.checkSignature(responseObject.getString("encrypted_token"), responseObject.getString("signature"))));
                token = pkm.calculateToken(responseObject.getString("encrypted_token"));
                Log.d("token", token);
            } catch (IOException e) {
                Log.d("Authentication error", "IO Error while sending request.");
                e.printStackTrace();
            } catch (JSONException e) {
                Log.d("Authentication error", "JSON error.");
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }
        }
    }

    private String getTokenFromKeyserver(String username, String password) {
        HttpsURLConnection connection = establishHttpsConnection("keyserver/authenticate");
        if (connection != null) {
            try {
                DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());
                JSONObject requestData = new JSONObject();
                requestData.put("name", username);
                requestData.put("password", password);
                requestStream.writeBytes(requestData.toString());
                requestStream.flush();
                requestStream.close();
                InputStream responseStream = connection.getInputStream();
                InputStreamReader responseReader = new InputStreamReader(responseStream);
                BufferedReader bufferedResponseReader = new BufferedReader(responseReader);
                String response = "";
                String responseLine;
                while ((responseLine = bufferedResponseReader.readLine()) != null) {
                    response += responseLine;
                }
                JSONObject responseObj = new JSONObject(response);
                return responseObj.getString("token");
            } catch (IOException e) {
                Log.d("Authentication error", "IO Error while sending request.");
                e.printStackTrace();
                return "";
            } catch (JSONException e) {
                Log.d("Authentication error", "Could not construct JSON for the request.");
                e.printStackTrace();
                return "";
            } finally {
                connection.disconnect();
            }
        } else {
            return "";
        }
    }

    private void registerKey(String token, PinytoKeyManager pkm, SharedPreferences settings) {
        JSONObject registerRequest = new JSONObject();
        try {
            registerRequest.put("token", token);
            registerRequest.put("public_key", pkm.getPublicKeyData());
        } catch (JSONException e) {
            Log.d("Registration error", "Unable to construct JSON data.");
            e.printStackTrace();
        }
        Log.d("register", registerRequest.toString());
        HttpsURLConnection connection = establishHttpsConnection("register_new_key");
        if (connection != null) {
            try {
                DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());
                requestStream.writeBytes(registerRequest.toString());
                requestStream.flush();
                requestStream.close();
                InputStream responseStream = connection.getInputStream();
                InputStreamReader responseReader = new InputStreamReader(responseStream);
                BufferedReader bufferedResponseReader = new BufferedReader(responseReader);
                String response = "";
                String responseLine;
                while ((responseLine = bufferedResponseReader.readLine()) != null) {
                    response += responseLine;
                }
                JSONObject responseObj = new JSONObject(response);
                if (responseObj.getBoolean("success")) {
                    Log.d("success", "registered new key");
                    pkm.saveKeysToSettings(settings);
                } else {
                    Log.d("Registration error", "Registration failed:");
                    Log.d("Registration error", responseObj.getString("error"));
                }
            } catch (IOException e) {
                Log.d("Registration error", "IO Error while sending request.");
                e.printStackTrace();
            } catch (JSONException e) {
                Log.d("Registration error", "The response seems to be no JSON.");
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }
        }
    }

    private void logout(String token) {
        JSONObject logoutRequest = new JSONObject();
        try {
            logoutRequest.put("token", token);
        } catch (JSONException e) {
            Log.d("Logout error", "Unable to construct JSON data.");
            e.printStackTrace();
        }
        HttpsURLConnection connection = establishHttpsConnection("logout");
        if (connection != null) {
            try {
                DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());
                requestStream.writeBytes(logoutRequest.toString());
                requestStream.flush();
                requestStream.close();
                InputStream responseStream = connection.getInputStream();
                InputStreamReader responseReader = new InputStreamReader(responseStream);
                BufferedReader bufferedResponseReader = new BufferedReader(responseReader);
                String response = "";
                String responseLine;
                while ((responseLine = bufferedResponseReader.readLine()) != null) {
                    response += responseLine;
                }
                try {
                    JSONObject responseObj = new JSONObject(response);
                    if (!responseObj.getBoolean("success")) {
                        Log.d("Logout error", "Logout at failed:");
                        Log.d("Logout error", responseObj.getString("error"));
                    }
                } catch (JSONException e) {
                    Log.d("Logout error", "The response seems to be no JSON:");
                    Log.d("Logout error", response);
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Log.d("Logout error", "IO Error while sending request.");
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }
        }
    }

    private String request(String path, JSONObject request) {
        HttpsURLConnection connection = establishHttpsConnection(path);
        if (connection != null) {
            try {
                DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());
                request.put("token", token);
                Log.d("request:", request.toString());
                requestStream.writeBytes(request.toString());
                requestStream.flush();
                requestStream.close();
                if (connection.getResponseCode() != 200) {
                    Log.d("response code", Integer.toString(connection.getResponseCode()));
                    Log.d("response message", connection.getResponseMessage());
                }
                InputStream responseStream = connection.getInputStream();
                InputStreamReader responseReader = new InputStreamReader(responseStream);
                BufferedReader bufferedResponseReader = new BufferedReader(responseReader);
                String response = "";
                String responseLine;
                while ((responseLine = bufferedResponseReader.readLine()) != null) {
                    response += responseLine;
                }
                Log.d(path + " response", response);
                return response;
            } catch (IOException e) {
                Log.d("Request error", "IO Error while sending request.");
                e.printStackTrace();
            } catch (JSONException e) {
                Log.d("Request error", "JSON error.");
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }
        }
        return "";
    }

    @Override
    protected String doInBackground(String... params) {
        if (token == null) {
            SharedPreferences settings = contentContext.getSharedPreferences(
                    "settings",
                    Context.MODE_PRIVATE);
            String username = settings.getString("username", "");
            username = "test";
            BigInteger N = new BigInteger(settings.getString("N", "0"));
            BigInteger e = new BigInteger(settings.getString("e", "0"));
            BigInteger d = new BigInteger(settings.getString("d", "0"));
            PinytoKeyManager pkm = new PinytoKeyManager(N, e, d);
            if (!pkm.keyExists()) {
                String keyserverToken = getTokenFromKeyserver(username, "123456");
                Log.d("keyserver token", keyserverToken);
                pkm.generateNewKeys();
                registerKey(keyserverToken, pkm, settings);
                logout(keyserverToken);
            }
            authenticate(username, pkm);
        }
        String path = params[0];
        try {
            JSONObject request = new JSONObject(params[1]);
            return request(path, request);
        } catch (JSONException e) {
            Log.d("Request error", "Request data is no JSON.");
            e.printStackTrace();
            return null;
        }
    }
}
