package com.sap.cisp.wt.websocket.config;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONObject;

import com.sap.cisp.wt.websocket.server.IOTAlarmAgentAnnotation;

public class Sluice {
    private String Id;
    private String latitude;
    private String longitude;
    private String sourceAddress;
    private SluiceStateEnum state;
    private static Log logger  = LogFactory.getLog(Sluice.class);
    
    public Sluice() {
        
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
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

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public SluiceStateEnum getState() {
        return state;
    }

    public void setState(SluiceStateEnum state) {
        SluiceStateEnum oldState = this.state;
        this.state = state;
        if( oldState != null && !oldState.getState().equalsIgnoreCase(state.getState())){
            JSONObject sluiceJson = new JSONObject();
            logger.info(">>> Sluice : " + getId() + " ; State change from :  " + getState() + " to : " + state.getState());
            sluiceJson.put("sluice_id", getId());
            sluiceJson.put("state", state.getState());
            sluiceJson.put("type", "sluice");
            IOTAlarmAgentAnnotation.broadcast(sluiceJson.toString());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((Id == null) ? 0 : Id.hashCode());
        result = prime * result
                + ((latitude == null) ? 0 : latitude.hashCode());
        result = prime * result
                + ((longitude == null) ? 0 : longitude.hashCode());
        result = prime * result
                + ((sourceAddress == null) ? 0 : sourceAddress.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Sluice other = (Sluice) obj;
        if (Id == null) {
            if (other.Id != null)
                return false;
        } else if (!Id.equals(other.Id))
            return false;
        if (latitude == null) {
            if (other.latitude != null)
                return false;
        } else if (!latitude.equals(other.latitude))
            return false;
        if (longitude == null) {
            if (other.longitude != null)
                return false;
        } else if (!longitude.equals(other.longitude))
            return false;
        if (sourceAddress == null) {
            if (other.sourceAddress != null)
                return false;
        } else if (!sourceAddress.equals(other.sourceAddress))
            return false;
        if (state != other.state)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Sluice [Id=" + Id + ", latitude=" + latitude + ", longitude="
                + longitude + ", sourceAddress=" + sourceAddress + ", state="
                + state + ", getClass()=" + getClass() + ", hashCode()="
                + hashCode() + ", toString()=" + super.toString() + "]";
    }

    public static enum SluiceStateEnum {
        Open("100"), SemiOpen("50"), Closed("0"), NA("Not Available");

        private final String state;

        private SluiceStateEnum(String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return state;
        }

        public String getState() {
            return state;
        }
    }
}
