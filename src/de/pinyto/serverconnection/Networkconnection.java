package de.pinyto.serverconnection;

import android.app.Activity;
import android.os.AsyncTask;

public class Networkconnection extends AsyncTask<String, Void, String>{
	Activity activity;
	
	public Networkconnection(Activity activity){
		this.activity = activity;
	}

	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		return "Hello";
	}
	
}
