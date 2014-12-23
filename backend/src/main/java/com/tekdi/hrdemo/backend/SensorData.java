package com.tekdi.hrdemo.backend;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * Created by fsd017 on 12/22/14.
 */
@Entity
public class SensorData {

    @Id
    Long id;
    String devRegId;
    String sensorData;
    String result;
    Text sensorText;
    String dummy;

    public Long getId() {
        return id;
    }

    public String getDevRegId() {
        return devRegId;
    }

    public String getSensorData() {
        return sensorData;
    }

    public String getResult() {
        return result;
    }

    public Text getSensorText() { return sensorText;}

    public void setDummy(String dummy) {
        this.dummy = dummy;
    }

    public String getDummy() {
        return dummy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDevRegId(String devRegId) {
        this.devRegId = devRegId;
    }

    public void setSensorData(String dataMap) {
        this.sensorData = dataMap;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setSensorText(Text sensorText) {
        this.sensorText = sensorText;
    }

}

