package com.iot.cisp.wt.websocket.config;

import java.util.HashMap;
import java.util.Map;

import com.iot.cisp.wt.util.Utils.StateEnum;


public class Station {
    @Override
    public String toString() {
        return "Station [latitude=" + latitude + ", longitude=" + longitude
                + ", sourceAddress=" + sourceAddress + ", id=" + id
                + ", isDummy=" + isDummy
                + ", state=" + state + ", toString()=" + super.toString() + "]";
    }

    private String latitude;
    private String longitude;
    private String sourceAddress; //XBee Mac address
    private String id;
    private Map<String, Sensor> sensorMap = new HashMap<String, Sensor>();
    private boolean isDummy = true;

    private StateEnum state;

    public Station() {
        
    }
    
    public Station(String id, String sourceAddress, String latitude, String longitude) {
        this.id = id;
        this.sourceAddress = sourceAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    
    public StateEnum getState() {
        return state;
    }

    public void setState(StateEnum state) {
        this.state = state;
    }
    
    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isDummy() {
        return isDummy;
    }

    public void setDummy(boolean isDummy) {
        this.isDummy = isDummy;
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + this.id.hashCode();
        result = 37 * result + this.latitude.hashCode();
        result = 37 * result + this.longitude.hashCode();
        result = 37 * result + this.sourceAddress.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        // a quick test to see if the objects are identical
        if (this == obj)
            return true;

        // must return false if the explicit parameter is null
        if (obj == null)
            return false;

        if (getClass() != obj.getClass()) {
            return false;
        }

        Station p = (Station) obj;

        if (this.id.equalsIgnoreCase(p.getId())
                && (this.getLatitude().equalsIgnoreCase(p.getLatitude()))
                && (this.getLongitude().equalsIgnoreCase(p.getLongitude()))
                && (this.getSourceAddress().equalsIgnoreCase(p.getSourceAddress()))) {
            return true;
        } else {
            return false;
        }
    }
    
   

    public Map<String, Sensor> getSensorMap() {
        return sensorMap;
    }

    public void setSensorMap(Map<String, Sensor> sensorMap) {
        this.sensorMap = sensorMap;
    }


}
