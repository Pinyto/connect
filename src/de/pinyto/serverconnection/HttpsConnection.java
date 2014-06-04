package de.pinyto.serverconnection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;
import de.pinyto.connect.R;

public class HttpsConnection extends AsyncTask<String, Void, String>{
	
	final Context context;
	Activity activity;
	
	SSLContext sslcontext;
	TrustManagerFactory tmf;
	
	String httpsURL = "https://cloud.pinyto.de/authenticate";
	
	public HttpsConnection(Context context, Activity activity){
		this.context = context;
		this.activity = activity;
	}

	@SuppressWarnings("finally")
	public KeyStore buildKeystore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		
		KeyStore trusted = KeyStore.getInstance("BKS");

		InputStream in = context.getResources().openRawResource(
				R.raw.mykeystorechain);
		System.out.println("using own SSL certificate");
		try {
			// Initialize the keystore with the provided trusted
			// certificates
			// Also provide the password of the keystore
			trusted.load(in, "changei".toCharArray());
		} finally {
			in.close();
			return trusted;
		}

	}
	
	
	
	public void buildTrustmanager() throws NoSuchAlgorithmException{
		
		String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
		try {
			tmf.init(buildKeystore());
		} catch (KeyStoreException e) {
			System.out.println("error: No keystore in BKS");
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("error: No Algorithm");
			e.printStackTrace();
		} catch (CertificateException e) {
			System.out.println("error: No certificate in keystore");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("error: Reading certificate unsuccessful");
			e.printStackTrace();
		}
	}
	
	public SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyManagementException{
		
		sslcontext = SSLContext.getInstance("TLS");
		sslcontext.init(null, tmf.getTrustManagers(), null);
		return sslcontext;
	}
	
	public String openconnection(String formdata) throws IOException{
		try {
			buildTrustmanager();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("error: No Algorithm to build trustmanager");
			e.printStackTrace();
		}
		
		try {
			createSSLContext();
		} catch (KeyManagementException e) {
			System.out.println("error: No key in trustmanager");
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("error: No Algorithm to build trustmanager");
			e.printStackTrace();
		}
			URL url = new URL(httpsURL);
			HttpsURLConnection urlConnection =
				    (HttpsURLConnection)url.openConnection();
			
			urlConnection.setSSLSocketFactory(sslcontext.getSocketFactory());
			urlConnection.setRequestMethod("POST");
			urlConnection.setRequestProperty("Host", "cloud.pinyto.de");
			urlConnection.setRequestProperty("Connection", "keep-alive");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			DataOutputStream printout = new DataOutputStream(urlConnection.getOutputStream());
			System.out.println("Get OutputStream");
			printout.writeBytes(formdata);
			System.out.println("Try to send formdata");
			printout.flush();
			printout.close();
			
			InputStream ins = urlConnection.getInputStream();
			InputStreamReader isr = new InputStreamReader(ins);

			BufferedReader in = new BufferedReader(isr);

			String response = new String();
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
				response += inputLine;
			}

			in.close();
			return response;
	}

	@Override
	protected String doInBackground(String... params) {
		String response = "no Token";
		String formdata = params[0];
		
		try {
			response = openconnection(formdata);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return response;
	}
	
	protected void onPostExecute(String result){
		TextView textView = (TextView) activity.findViewById(R.id.token);
		textView.setText(result);
	}
}
