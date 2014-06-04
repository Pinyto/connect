package de.pinyto.connect;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import de.pinyto.serverconnection.HttpsConnection;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
	
	
	
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		/**TODO
		 * getFragmentManager().beginTransaction()
		 * .replace(android.R.id.content, new SettingsFragment()) .commit();
		 **/
		setContentView(R.layout.activity_main);		

	}
	
	public Context getAppContext(){
		return this.getBaseContext();
	}
	
	/**
	 * 
	 * @return Formdata like username=test&keyhash=1234567890 
     * @throws NoSuchAlgorithmException if SHA-256 could not be used as algorithm
	 * @throws UnsupportedEncodingException if hex-coding is not possible
	 */
	protected String getUserdata() throws NoSuchAlgorithmException, UnsupportedEncodingException{
		User user = new User(this);	
		return user.createFormdata();
	}
	
	/**
	 * starts the thread which connects to the internet
     * @throws NoSuchAlgorithmException if SHA-256 could not be used as algorithm
	 * @throws UnsupportedEncodingException if hex-coding is not possible
	 */	
	protected void openConnection() throws NoSuchAlgorithmException, UnsupportedEncodingException{
		String formdata = getUserdata();
		Context context = getAppContext();
		HttpsConnection networkconnection = new HttpsConnection(context, this);
		networkconnection.execute(formdata);
	}
	
	protected void hasConnection(){
		//TODO Check if phone is connected to the internet
	}
	
	public void onClick(View view){
		try {
			openConnection();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("SHA-256 no available Algorithm");
		} catch (UnsupportedEncodingException e) {
			System.out.println("Could not encode to Hex");
		}
	}
}


