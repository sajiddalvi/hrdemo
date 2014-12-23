package com.tekdi.hrdemo;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.tekdi.hrdemo.backend.sensorDataApi.model.SensorData;

public class WatchListenerService extends WearableListenerService {

    private static final String TAG = "HR_DEMO_PHONE";
    private WatchConnection mWatchConnection ;

    @Override
    public void onCreate(){
        super.onCreate();
        Log.v(TAG, "WearableListenerService OnCreate ");
        mWatchConnection = new WatchConnection(this.getApplicationContext());

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.v(TAG, "onDataChanged");
        DataMap dataMap;
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                Log.v(TAG, "DataMap received on phone: " + dataMap.toString());

                SensorDataEndpointAsyncTask l = new SensorDataEndpointAsyncTask(this);
                SensorData data = new SensorData();
                String str = dataMap.toString();
                data.setSensorData(str);
                data.setDevRegId("reg");
                data.setResult("working");

                Log.v(TAG,"onDataChanged "+dataMap.toString());

                l.execute(new Pair<Context, SensorData>(this, data));

            }
        }
    }

    public void onServerDataReceived(String result){
        Log.v(TAG,"onServerDataReceived:"+result);
        mWatchConnection.sendMessage("/serverdata",result);

    }
}