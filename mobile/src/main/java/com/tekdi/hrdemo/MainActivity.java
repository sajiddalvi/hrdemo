package com.tekdi.hrdemo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "HR_DEMO_PHONE";
    private WatchConnection mWatchConnection ;
    //private String devRegId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "starting phone app");

        if ((Prefs.getDeviceRegIdPref(this)) == "") {
            Log.v(TAG,"register new device");
            new GcmRegistrationAsyncTask(this).execute();
        }

        mWatchConnection = new WatchConnection(this.getApplicationContext());

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void registrationDone(String regId) {
        Log.v(TAG,"registration done " + regId);
        Prefs.setDeviceIdRegPref(this,regId);
        //this.setDevRegId(regId);
        //mWatchConnection.sendMessage("/registration","success");
    }
}
