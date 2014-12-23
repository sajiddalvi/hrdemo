package com.tekdi.hrdemo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SensorEventListener,
        MessageApi.MessageListener {

    private TextView mTextView;
    private GoogleApiClient googleClient;
    private static final String TAG = "HR_DEMO_WATCH";
    private SensorManager mSensorManager;
    private Sensor mAccel;
    private boolean mConnected = false;
    private boolean mRegistered = false;
    private static final Integer MAX_BUFFER = 10;
    private Integer mBufferCount;
    private ArrayList<DataMap> mBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });


        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mBuffer = new ArrayList<DataMap>();
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();
    }

    // Send a data object when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {

        Log.v(TAG, "onConnected");

        mConnected = true;

        Wearable.MessageApi.addListener(googleClient, this);

    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            Log.v(TAG, "running watch activity");

            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
            for (Node node : nodes.getNodes()) {

                // Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient,request).await();
                if (result.getStatus().isSuccess()) {
                    Log.v(TAG, "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                    //mBuffer.clear();
                } else {
                    // Log an error
                    Log.v(TAG, "ERROR: failed to send DataMap");
                }
            }
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        Float x = event.values[0];
        Float y = event.values[1];
        Float z = event.values[2];
        Long timestamp = new Date().getTime();


        if ((mConnected == true) && (mRegistered == true)) {
            Log.v(TAG,x.toString()+","+y.toString()+","+z.toString());

            DataMap data = new DataMap();
            data.putLong("time", new Date().getTime());
            data.putFloat("x", x);
            data.putFloat("y", y);
            data.putFloat("z", z);
            mBuffer.add(data);

            if (mBuffer.size() == MAX_BUFFER) {
                DataMap dataMap = new DataMap();
                dataMap.putDataMapArrayList("sensor_data_buffer", mBuffer);
                new SendToDataLayerThread("/data", dataMap).start();

            }
        }

        // Do something with this sensor value.
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
        mBufferCount = 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/registration")) {
            Log.v(TAG,"registered with server");
            mRegistered = true;
        }
        if (messageEvent.getPath().equals("/serverdata")) {
            Log.v(TAG,"got data back from server.");
            mSensorManager.unregisterListener(this);
        }
    }
}
