package com.iot.cisp.wt.websocket.config;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.iot.cisp.wt.websocket.config.Sensor.SensorTypeEnum;
import com.iot.cisp.wt.websocket.server.AlarmAgent;

@SuppressWarnings("rawtypes")
public class Alarm implements Comparable{
    private String stationId;
    private String description;
    private AlarmServerityEnum severity;
    private String sensorId;
    private AlarmActionEnum action;
    private AlarmCauseEnum cause;
    private AlarmStateEnum state;
    private static Log logger  = LogFactory.getLog(Alarm.class);
    private double value;
    private SensorTypeEnum type;

    public Alarm() {
         // default state is active
         this.state = AlarmStateEnum.Active;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cause == null) ? 0 : cause.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result
                + ((sensorId == null) ? 0 : sensorId.hashCode());
        result = prime * result
                + ((severity == null) ? 0 : severity.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result
                + ((stationId == null) ? 0 : stationId.hashCode());
        return result;
    }



    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Alarm)) {
            return false;
        }
        Alarm other = (Alarm) obj;
        if (cause != other.cause) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (sensorId == null) {
            if (other.sensorId != null) {
                return false;
            }
        } else if (!sensorId.equals(other.sensorId)) {
            return false;
        }
        if (severity != other.severity) {
            return false;
        }
        if (stationId == null) {
            if (other.stationId != null) {
                return false;
            }
        } else if (!stationId.equals(other.stationId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Alarm [stationId=" + stationId + ", description=" + description
                + ", severity=" + severity + ", sensorId=" + sensorId
                + ", action=" + action + ", cause=" + cause + ", state="
                + state + ", value=" + value + ", type=" + type + "]";
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getDescription() {
        return getDescription(cause);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity.getServerity();
    }

    public void setSeverity(AlarmServerityEnum severity) {
        this.severity = severity;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }
    
    public AlarmActionEnum getAction() {
        return action;
    }

    public void setAction(AlarmActionEnum action) {
        this.action = action;
    }
    
    public AlarmCauseEnum getCause() {
        return cause;
    }

    public void setCause(AlarmCauseEnum cause) {
        this.cause = cause;
    }
    

    public synchronized AlarmStateEnum getState() {
        return state;
    }

    public synchronized void  setState(AlarmStateEnum state) {
        // alarm state change, for clear use case
        if(! this.state.getState().equalsIgnoreCase(state.getState())) {
            logger.info("*** Alarm state changed. From state:  " + this.state.getState() + " To state: " + state.getState() + ". " + this.toString());
            this.state = state;
            logger.info("Send message to websocket queue.");
            AlarmAgent.addAlarmToQueue(this);
            return;
        }
        logger.info("*** Alarm state unchanged. "  + this.toString());
        this.state = state;
    }

    public double getValue() {
        return value;
    }



    public void setValue(double value) {
        this.value = value;
    }



    public SensorTypeEnum getType() {
        return type;
    }



    public void setType(SensorTypeEnum type) {
        this.type = type;
    }
    
    public String getDescription(AlarmCauseEnum cause) {
        switch(cause){
        case PH_LOW_WARN:
            return "The PH value exceed the low warning threshold.";
        case PH_LOW_CRITICAL:
            return "The PH value exceed the low critical threshold.";
        case PH_HIGH_WARN:
            return "The PH value exceed the high warning threshold.";
        case PH_HIGH_CRITICAL:
            return "The PH value exceed the high critical threshold.";
        case TEMPERATURE_LOW_WARN:
            return "The temperature value exceed the low warning threshold.";
        case TEMPERATURE_LOW_CRITICAL:
            return "The temperature value exceed the low critical threshold.";
        case TEMPERATURE_HIGH_WARN:
            return "The temperature value exceed the high warning threshold.";
        case TEMPERATURE_HIGH_CRITICAL:
            return "The temperature value exceed the high critical threshold.";
        case FLOW_LOW_WARN:
            return "The water flow value exceed the low warning threshold.";
        case FLOW_LOW_CRITICAL:
            return "The water flow value exceed the low critical threshold.";  
        case FLOW_HIGH_WARN:
            return "The water flow value exceed the high warning threshold.";
        case FLOW_HIGH_CRITICAL:
            return "The water flow value exceed the high critical threshold.";
        case LEVEL_LOW_WARN:
            return "The water level value exceed the low warning threshold.";
        case LEVEL_LOW_CRITICAL:
            return "The water level value exceed the low critical threshold.";
        case LEVEL_HIGH_WARN:
            return "The water level value exceed the high warning threshold.";
        case LEVEL_HIGH_CRITICAL:
            return "The water level value exceed the high critical threshold.";
        default:
            return "Unknown.";
        }
    }

    public static enum AlarmActionEnum {
        Raise("raise"), Clear("clear");

        private final String action;

        private AlarmActionEnum(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return action;
        }

        public String getAction() {
            return action;
        }
    }
    
    public static enum AlarmServerityEnum {
        Warn("warn"), Critical("critical"), None("None");

        private final String serverity;

        private AlarmServerityEnum(String serverity) {
            this.serverity = serverity;
        }

        @Override
        public String toString() {
            return serverity;
        }

        public String getServerity() {
            return serverity;
        }
    }
    
    public static enum AlarmStateEnum {
        Active("active"), Cleared("cleared");

        private final String state;

        private AlarmStateEnum(String state) {
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
    
    public static enum AlarmCauseEnum {
        PH_LOW_WARN(0), PH_LOW_CRITICAL(1), PH_HIGH_WARN(2), PH_HIGH_CRITICAL(3),
        TEMPERATURE_LOW_WARN(4), TEMPERATURE_LOW_CRITICAL(5), TEMPERATURE_HIGH_WARN(6), TEMPERATURE_HIGH_CRITICAL(7),
        FLOW_LOW_WARN(8), FLOW_LOW_CRITICAL(9), FLOW_HIGH_WARN(10), FLOW_HIGH_CRITICAL(11),
        LEVEL_LOW_WARN(12), LEVEL_LOW_CRITICAL(13), LEVEL_HIGH_WARN(14), LEVEL_HIGH_CRITICAL(15), NONE(99);

        private final int cause;

        private AlarmCauseEnum(int cause) {
            this.cause = cause;
        }

        @Override
        public String toString() {
            return Integer.toString(cause);
        }

        public int getCause() {
            return cause;
        }
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Alarm) {  
            // int cmp = Double.compare(number, ((Pair) o).number);  
            int station = Integer.parseInt(stationId) - Integer.parseInt(((Alarm) o).getStationId());  
            if (station != 0) {// station id first
                return station;  
            }  
            int sensor = Integer.parseInt(sensorId)- Integer.parseInt(((Alarm) o).getSensorId());
            if(sensor != 0) { //sensor id second
                return sensor;
            }
            int severityCmp  = severity.getServerity().compareTo(((Alarm) o).getSeverity());
            if(severityCmp != 0) { // severity third
                return severityCmp;
            }
            return cause.compareTo(((Alarm) o).getCause());  
        }  
        throw new ClassCastException("Cannot compare Alarm with "  
                + o.getClass().getName());  
    }
}
