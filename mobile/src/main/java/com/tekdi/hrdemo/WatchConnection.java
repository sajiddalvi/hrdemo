package com.tekdi.hrdemo;

import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by fsd017 on 12/23/14.
 */
public class WatchConnection {

    private static GoogleApiClient mApiClient = null;


    public WatchConnection(Context context) {
        if (mApiClient == null) {
            mApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .build();

            mApiClient.connect();
        }
    }

    public void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

}
