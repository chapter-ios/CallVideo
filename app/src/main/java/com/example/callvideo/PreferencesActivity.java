package com.example.callvideo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.InputType;

import com.example.callvideo.util.Constants;

public class PreferencesActivity extends AppCompatActivity {
    //SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.preferences);

        /*
        this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);

        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.remove(Constants.SERVER_NAME);
        edit.apply();
        */

//        EditTextPreference pref = (EditTextPreference)find(Constants.SERVER_NAME);
//        pref.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
    }
}
