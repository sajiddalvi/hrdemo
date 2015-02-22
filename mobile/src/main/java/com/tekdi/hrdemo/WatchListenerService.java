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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WatchListenerService extends WearableListenerService {

    private static final String TAG = "HR_DEMO_PHONE";
    private WatchConnection mWatchConnection ;
    private static ArrayList<DataMap> mDataMapBuffer = new ArrayList<DataMap>();
    private static ArrayList<Integer> mDataBuffer = new ArrayList<Integer>();
    public static final int MAX_CHUNK_SIZE = 1000;
    public static final int FIRST_CHUNK_SEQ_NUM = 0;
    public static final int LAST_CHUNK_SEQ_NUM = 99;
    private static int mChunkCount = 0;

    @Override
    public void onCreate(){
        super.onCreate();
        Log.v(TAG, "WearableListenerService OnCreate ");
        mWatchConnection = new WatchConnection(this.getApplicationContext());
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        String regId;

        Log.v(TAG, "onDataChanged");

        if ((regId = Prefs.getDeviceRegIdPref(this)) == "") {
            Log.v(TAG,"waiting for registration");
        }
        else {
            DataMap dataMap;
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {

                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                    Log.v(TAG, "DataMap received on phone: " + dataMap.getInt("sensor_chunk_seq_num"));

                    mDataMapBuffer.add(dataMap);

                    mChunkCount ++;
                    Log.v(TAG,"chunkcount="+mChunkCount);

                    if (mChunkCount == 5) {

                        ArrayList<DataMap> sortedDataMapBuffer = new ArrayList<DataMap>();
                        for (DataMap cdm : mDataMapBuffer) {
                            int seqNum = cdm.getInt("sensor_chunk_seq_num");
                            if (seqNum == 0) {
                                sortedDataMapBuffer.add(cdm);
                                break;
                            }
                        }
                        for (DataMap cdm : mDataMapBuffer) {
                            int seqNum = cdm.getInt("sensor_chunk_seq_num");
                            if (seqNum == 1) {
                                sortedDataMapBuffer.add(cdm);
                                break;
                            }
                        }
                        for (DataMap cdm : mDataMapBuffer) {
                            int seqNum = cdm.getInt("sensor_chunk_seq_num");
                            if (seqNum == 2) {
                                sortedDataMapBuffer.add(cdm);
                                break;
                            }
                        }
                        for (DataMap cdm : mDataMapBuffer) {
                            int seqNum = cdm.getInt("sensor_chunk_seq_num");
                            if (seqNum == 3) {
                                sortedDataMapBuffer.add(cdm);
                                break;
                            }
                        }
                        for (DataMap cdm : mDataMapBuffer) {
                            int seqNum = cdm.getInt("sensor_chunk_seq_num");
                            if (seqNum == 4) {
                                sortedDataMapBuffer.add(cdm);
                                break;
                            }
                        }

                        for (DataMap cdm : sortedDataMapBuffer) {
                            ArrayList<DataMap> dataMapBuffer = new ArrayList<DataMap>();
                            dataMapBuffer = cdm.getDataMapArrayList("sensor_data_buffer");

                            for (DataMap dm : dataMapBuffer) {
                                Integer data = dm.getInt("x");
                                mDataBuffer.add(data);
                            }
                        }


                        SensorDataEndpointAsyncTask l = new SensorDataEndpointAsyncTask(this);
                        SensorData data = new SensorData();

                        JSONArray jsonArray = new JSONArray();


                        for (Integer dm : mDataBuffer) {
                            JSONObject obj= new JSONObject();
                            try {
                                obj.put("x",dm);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            jsonArray.put(obj);
                        }

                        JSONObject object = new JSONObject();
                        try {
                            object.put("regId",regId);
                            object.put("result", "working");
                            object.put("sensor_data_buffer",(Object)jsonArray);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String str = object.toString();
                        data.setSensorData(str);
                        data.setDevRegId(regId);
                        data.setResult("working");

                        Log.v(TAG, "onDataChanged " + dataMap.toString());

                        l.execute(new Pair<Context, SensorData>(this, data));


                        sortedDataMapBuffer.clear();
                        mDataMapBuffer.clear();
                        mDataBuffer.clear();
                        mChunkCount = 0;
                    }

                }
            }
        }
    }

    public void onServerDataReceived(String result){
        Log.v(TAG,"onServerDataReceived:"+result);
        mWatchConnection.sendMessage("/serverdata",result);

    }
}