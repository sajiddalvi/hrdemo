package com.tekdi.hrdemo;

import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;

/**
 * Created by fsd017 on 1/21/15.
 */
public class SensorChunk {

    public static final int MAX_CHUNK_SIZE = 1000;
    public static final int FIRST_CHUNK_SEQ_NUM = 0;
    public static final int LAST_CHUNK_SEQ_NUM = 99;

    private int seqNum;
    private ArrayList<DataMap> data;

    SensorChunk() {
        this.seqNum = FIRST_CHUNK_SEQ_NUM;
        this.data = new ArrayList<DataMap>();
    }

    public boolean add(int value) {
        if (this.data.size() < MAX_CHUNK_SIZE) {
            DataMap dm = new DataMap();
            dm.putInt("x", value);
            this.data.add(dm);
            return true;
        } else {
            return false;
        }
    }

    public void clear() {
        this.data.clear();
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public int getSeqNum() {
        return this.seqNum;
    }

    public ArrayList<DataMap> getData() {
        return this.data;
    }
}
