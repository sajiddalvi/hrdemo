package com.tekdi.hrdemo;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.tekdi.hrdemo.backend.sensorDataApi.SensorDataApi;
import com.tekdi.hrdemo.backend.sensorDataApi.model.SensorData;

import java.io.IOException;

/**
 * Created by fsd017 on 12/22/14.
 */
public class SensorDataEndpointAsyncTask extends AsyncTask<Pair<Context, SensorData>, Void, String> {

    private static SensorDataApi myApiService = null;
    private Context context;
    private static final String TAG = "HR_DEMO_PHONE";
    private WatchListenerService caller;

    SensorDataEndpointAsyncTask(WatchListenerService caller) {
        Log.v(TAG,"in creator SensorDataEndpointAsyncTask");
        this.caller = caller;

    }

    @Override
    protected String doInBackground(Pair<Context, SensorData>... params) {
        if(myApiService == null) {  // Only do this once
            SensorDataApi.Builder builder = new SensorDataApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null)
                    .setRootUrl("https://xenon-coast-802.appspot.com/_ah/api/")
                    .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                        @Override
                        public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                            abstractGoogleClientRequest.setDisableGZipContent(true);
                        }
                    });
            myApiService = builder.build();
        }

        context = params[0].first;
        SensorData data = params[0].second;

        Log.v(TAG,"SensorDataEndpointAsyncTask"+data.toString());

        try {
            SensorData sensorData =myApiService.insert(data).execute();
            return sensorData.getResult();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Log.v(TAG,"onPostExecute sensor data result = "+result);
        Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        caller.onServerDataReceived(result);
    }

}
