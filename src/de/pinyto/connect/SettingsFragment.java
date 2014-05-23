package de.pinyto.connect;

import android.preference.PreferenceFragment;
import android.os.Bundle;

public class SettingsFragment extends PreferenceFragment{
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}
