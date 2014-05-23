package de.pinyto.connect;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();

    }

}
