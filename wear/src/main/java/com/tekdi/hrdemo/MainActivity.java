package com.tekdi.hrdemo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
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
    private TextView mResultTextView;

    private GoogleApiClient googleClient;
    private static final String TAG = "HR_DEMO_WATCH";
    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mAds;
    private boolean mConnected = false;
    private static final Integer MAX_BUFFER = 25;
    private ArrayList<DataMap> mBuffer;

    private Handler mHandler;
    
    private static int mCurrAdsChunkNumber = -1;
    private static SensorChunk mSensorChunk;


    private static boolean waitForTransferComplete;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mResultTextView = (TextView) stub.findViewById(R.id.text);
                //mResultTextView = (TextView) stub.findViewById(R.id.result);
                mResultTextView.setText("initialized");
            }
        });


        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mCurrAdsChunkNumber = -1;
        mAds = mSensorManager.getDefaultSensor(0x10004);

        mBuffer = new ArrayList<DataMap>();

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {

                Log.v(TAG,"handleMessage");
                if (inputMessage.what == 99){
                    mResultTextView.setText(inputMessage.obj.toString());
                }

            }
        };

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
        mResultTextView.setText("connected");

        Wearable.MessageApi.addListener(googleClient, this);

        mSensorManager.registerListener(this,mAds,SensorManager.SENSOR_DELAY_FASTEST);

        //sendData();

    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
            Wearable.MessageApi.removeListener(googleClient, this);
            mResultTextView.setText("dis-connected");

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
                    Log.v(TAG, "DataMap:"+dataMap.getInt("sensor_chunk_seq_num"));
                   // Log.v(TAG, "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
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


        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                Float x = event.values[0];
                Float y = event.values[1];
                Float z = event.values[2];
                Long timestamp = new Date().getTime();


                if ((mConnected == true)) {

                    mResultTextView.setText("getting data");

                    Log.v(TAG, x.toString() + "," + y.toString() + "," + z.toString());

                    DataMap data = new DataMap();
                    data.putLong("time", new Date().getTime());
                    data.putFloat("x", x);
                    data.putFloat("y", y);
                    data.putFloat("z", z);
                    mBuffer.add(data);

                    if (mBuffer.size() == MAX_BUFFER) {
                        mResultTextView.setText("send to server");
                        DataMap dataMap = new DataMap();
                        dataMap.putDataMapArrayList("sensor_data_buffer", mBuffer);
                        new SendToDataLayerThread("/data", dataMap).start();
                        mSensorManager.unregisterListener(this);
                        //mBuffer.clear();

                    }
                }
                break;
            
            case 0x10004:

                if ((mConnected == true))
                 {
                    // Get the ADS Value
                    Integer adsValue = (int) event.values[0];

                    Log.v(TAG, "ads_value=" + adsValue + ",seq=" + event.values[1]);

                    // The first time after registration is a special case
                    if (mCurrAdsChunkNumber == -1) {
                        mResultTextView.setText("chunk 0");
                        Log.v(TAG, "setting up chunk 0");
                        mCurrAdsChunkNumber = 0;

                        mResultTextView.setText("chunk " + mCurrAdsChunkNumber);
                        mSensorChunk = new SensorChunk();
                        mSensorChunk.setSeqNum(mCurrAdsChunkNumber);
                        mSensorChunk.add(adsValue);
                    } else {
                        // add value to chunk
                        if (!mSensorChunk.add(adsValue)) {
                            Log.v(TAG, "chunk full .. sending data");
                            // if it fails, it means the chunk is full
                            // send the chunk out, before getting a new one
                        

                        DataMap dataMap = new DataMap();
                        dataMap.putInt("sensor_chunk_seq_num", mSensorChunk.getSeqNum());
                        dataMap.putDataMapArrayList("sensor_data_buffer", mSensorChunk.getData());

                        new SendToDataLayerThread("/data", dataMap).start();


                            // get a new sensorChunk
                            mCurrAdsChunkNumber++;
                            mResultTextView.setText("chunk " + mCurrAdsChunkNumber);

                            // currently just support 10 secs worth of data
                            // each chunk has 2 secs worth of data
                            if (mCurrAdsChunkNumber == 5) {
                                Log.v(TAG, "done with sampling");
                                mSensorManager.unregisterListener(this);
                                mResultTextView.setText("done sampling");

                            } else {
                                Log.v(TAG, "new chunk " + mCurrAdsChunkNumber);
                                mSensorChunk = new SensorChunk();
                                mSensorChunk.setSeqNum(mCurrAdsChunkNumber);
                                mSensorChunk.add(adsValue);
                            }
                        }
                    }
                }
                break;
        }
    }

    private void sendData() {

        mResultTextView.setText("send to server");

        String rData[] = RawSensorData.data;

        int count = 0;
        int chunkSeqNum = 0;
        SensorChunk sensorChunk = new SensorChunk();

        Log.v(TAG,"count="+count+",type="+sensorChunk.getSeqNum());

        for (String s : rData) {

            // try to add data to chunk
            // if the add fails, it means the chunk has reached its max size

            if (! sensorChunk.add(Integer.parseInt(s))) {
                DataMap dataMap = new DataMap();
                dataMap.putInt("sensor_chunk_seq_num",sensorChunk.getSeqNum());
                dataMap.putDataMapArrayList("sensor_data_buffer", sensorChunk.getData());


                new SendToDataLayerThread("/data", dataMap).start();

                // get a new sensorChunk
                sensorChunk = new SensorChunk();
                sensorChunk.setSeqNum(++chunkSeqNum);
                sensorChunk.add(Integer.parseInt(s));

            }

            count ++;

        }
        // send the last chunk to the phone
        DataMap dataMap = new DataMap();
        dataMap.putInt("sensor_chunk_seq_num",chunkSeqNum);
        dataMap.putDataMapArrayList("sensor_data_buffer", sensorChunk.getData());

        // send the existing chunk to the phone
        new SendToDataLayerThread("/data", dataMap).start();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);

        //mSensorManager.registerListener(this,mAds,SensorManager.SENSOR_DELAY_FASTEST);

        //mSensorManager.registerListener(this,mAds,SensorManager.SENSOR_DELAY_FASTEST); //amitj
        mCurrAdsChunkNumber = -1;
        mBuffer.clear();

    }

    @Override
    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
        //mBuffer.clear();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        final String message = new String(messageEvent.getData());

        if (messageEvent.getPath().equals("/serverdata")) {
            Log.v(TAG,"got data back from server : "+message);

            mHandler.sendMessage(mHandler.obtainMessage(99,message));
            //mSensorManager.unregisterListener(this);
        }
    }
}
