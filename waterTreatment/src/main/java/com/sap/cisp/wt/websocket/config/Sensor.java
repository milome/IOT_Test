package com.sap.cisp.wt.websocket.config;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.rapplogic.xbee.api.XBeeException;
import com.sap.cisp.wt.util.Utils.StateEnum;
import com.sap.cisp.wt.websocket.config.Alarm.AlarmCauseEnum;
import com.sap.cisp.wt.websocket.config.Alarm.AlarmServerityEnum;
import com.sap.cisp.wt.websocket.server.AlarmAgent;
import com.sap.cisp.wt.websocket.server.IOTAlarmAgentAnnotation;
import com.sap.cisp.wt.websocket.server.IOTSensorDataAnnotation;
import com.sap.cisp.wt.websocket.server.StationAgent;

public class Sensor {
    private Station station;
    private SensorTypeEnum type;
    private double value;
    private double upThresholdWarning;
    private double downThresholdWarning;
    private double upThresholdError;
    private double downThresholdError;
    private String Id;
    private StateEnum state;
    private static Log logger = LogFactory.getLog(Sensor.class);

    // PH offset
    // liudao
    // ph 9.18 : 9.40
    // ph 6.86 : 6.88
    // ph 4.0 : 4.12

    // dfrobot
    // 校准7.00: 6.87, offset + 0.13
    // ph 9.18 : 9.18
    // ph 4.0 : 3.90
    // ph6.86 : 6.80
    public Sensor() {

    }

    public Sensor(Station station, SensorTypeEnum type) {
        this.station = station;
        this.type = type;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public SensorTypeEnum getType() {
        return type;
    }

    public void setType(SensorTypeEnum type) {
        this.type = type;
    }

    public synchronized double getValue() {
        return value;
    }

    public synchronized void setValue(double value) {
        this.value = value;
        // Need to consider the anti-shaking
        // Need to consider the correlation, such as warn + error = error only
        // Normal value. no need to raise alarm
        logger.info(">>> Sensor id: " + this.getId()
                + " ; Genrate alarm severity : "
                + generateAlarmServerity(value).getServerity());
        if (generateAlarmServerity(value).getServerity().equalsIgnoreCase(
                AlarmServerityEnum.None.getServerity())) {
            // Check if there is previous alarm in map, if yes, clear it.
            logger.info("### The sensor : " + Id
                    + " has recovered to normal. Clear the alarms.");
            // How to de-bounce???
            AlarmAgent.clearSensorAlarm(station.getId(), Id);
            setState(StateEnum.Green);
            /* For demo purpose, when clear the PH alarm on station 12, clear the PH alarm on station 11 as well 
             * Need to consider if need additional logic to clear fake alarm when real is changed to warning? */
            if(station.getId().equalsIgnoreCase(
                    Integer.toString(IOTSensorDataAnnotation.demoSourceStationId))
                    && Id.equalsIgnoreCase(Integer
                            .toString((IOTSensorDataAnnotation.demoSourceStationId - 1) * 4 + 1))) {
                int fakeAlarmStationId = IOTSensorDataAnnotation.demoSourceStationId - 1;
                int fakeAlarmPHSensorId = (fakeAlarmStationId - 1) * 4 + 1;
                AlarmAgent.clearSensorAlarm(Integer.toString(fakeAlarmStationId), Integer.toString(fakeAlarmPHSensorId));
                Station station = StationAgent.stationIdMap.get(Integer.toString(fakeAlarmStationId));  
                logger.info("*** Check the fake alarm sensor : " + station.getSensorMap().get(Integer.toString(fakeAlarmPHSensorId)));
                station.getSensorMap().get(Integer.toString(fakeAlarmPHSensorId)).setState(StateEnum.Green);
                //Reset the sluices here
                logger.info("*** Auto reset the sluices!!! ****");
                try {
                    IOTAlarmAgentAnnotation.resetCommand();
                    StationAgent.setResetNeeded(false);
                } catch (XBeeException e) {
                    // TODO Auto-generated catch block
                    logger.error(e);
                }
            }
        } else {
            Alarm alarm = new Alarm();
            alarm.setStationId(station.getId());
            alarm.setSensorId(Id);
            alarm.setSeverity(generateAlarmServerity(value));
            alarm.setCause(generateAlarmCause(value,
                    generateAlarmServerity(value)));
            alarm.setDescription(generateAlarmDescription(alarm));
            alarm.setValue(value);
            alarm.setType(type);
            AlarmAgent.raiseAlarm(alarm);
            if(alarm.getSeverity().equalsIgnoreCase(AlarmServerityEnum.Critical.getServerity())) {
                setState(StateEnum.Red);
            } else {
                if(alarm.getSeverity().equalsIgnoreCase(AlarmServerityEnum.Warn.getServerity())) {
                    setState(StateEnum.Yellow);
                }
            }
            /* For demo purpose, when raise a PH critical alarm on station 12,
            generate a fake warning alarm on station 11. */
            if (station.getId().equalsIgnoreCase(
                    Integer.toString(IOTSensorDataAnnotation.demoSourceStationId))
                    && Id.equalsIgnoreCase(Integer
                            .toString((IOTSensorDataAnnotation.demoSourceStationId - 1) * 4 + 1))
                    && (alarm.getSeverity().equalsIgnoreCase(AlarmServerityEnum.Critical.getServerity()))) {
                Alarm alarmFake = new Alarm();
                int fakeAlarmStationId = IOTSensorDataAnnotation.demoSourceStationId - 1;
                int fakeAlarmPHSensorId = (fakeAlarmStationId - 1) * 4 + 1;
                alarmFake.setStationId(Integer.toString(fakeAlarmStationId));
                alarmFake.setSensorId(Integer.toString(fakeAlarmPHSensorId));
                alarmFake.setSeverity(AlarmServerityEnum.Warn);
                Station station = StationAgent.stationIdMap.get(Integer.toString(fakeAlarmStationId));
                station.getSensorMap().get(Integer.toString(fakeAlarmPHSensorId)).setState(StateEnum.Yellow);
                if(alarm.getCause().equals(AlarmCauseEnum.PH_HIGH_CRITICAL)) {
                    alarmFake.setCause(AlarmCauseEnum.PH_HIGH_WARN);
                    alarmFake.setValue(IOTSensorDataAnnotation.fakePHSodaWarnVal);
                } else {
                    alarmFake.setCause(AlarmCauseEnum.PH_LOW_WARN);
                    alarmFake.setValue(IOTSensorDataAnnotation.fakePHAcidWarnVal);
                }
                alarmFake.setDescription(generateAlarmDescription(alarmFake)); 
                alarmFake.setType(type);
                try {
                    Thread.sleep(IOTSensorDataAnnotation.fakeAlarmDelay);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                AlarmAgent.raiseAlarm(alarmFake);
            }
        }

    }

    public double getUpThresholdWarning() {
        return upThresholdWarning;
    }

    public void setUpThresholdWarning(double upThresholdWarning) {
        this.upThresholdWarning = upThresholdWarning;
    }

    public double getDownThresholdWarning() {
        return downThresholdWarning;
    }

    public void setDownThresholdWarning(float downThresholdWarning) {
        this.downThresholdWarning = downThresholdWarning;
    }

    public double getUpThresholdError() {
        return upThresholdError;
    }

    public void setUpThresholdError(double upThresholdError) {
        this.upThresholdError = upThresholdError;
    }

    public double getDownThresholdError() {
        return downThresholdError;
    }

    public void setDownThresholdError(double downThresholdError) {
        this.downThresholdError = downThresholdError;
    }

    public AlarmCauseEnum generateAlarmCause(double value,
            AlarmServerityEnum serverity) {
        switch (type) {
        case PH:
            switch (serverity) {
            case Critical:
                if (value >= upThresholdError) {
                    return AlarmCauseEnum.PH_HIGH_CRITICAL;
                } else {
                    return AlarmCauseEnum.PH_LOW_CRITICAL;
                }
            case Warn:
                if (value >= upThresholdWarning && value < upThresholdError) {
                    return AlarmCauseEnum.PH_HIGH_WARN;
                } else {
                    return AlarmCauseEnum.PH_LOW_WARN;
                }
            default:
                return AlarmCauseEnum.NONE;
            }
        case Temperature:
            switch (serverity) {
            case Critical:
                if (value >= upThresholdError) {
                    return AlarmCauseEnum.TEMPERATURE_HIGH_CRITICAL;
                } else {
                    return AlarmCauseEnum.TEMPERATURE_LOW_CRITICAL;
                }
            case Warn:
                if (value >= upThresholdWarning && value < upThresholdError) {
                    return AlarmCauseEnum.TEMPERATURE_HIGH_WARN;
                } else {
                    return AlarmCauseEnum.TEMPERATURE_LOW_WARN;
                }
            default:
                return AlarmCauseEnum.NONE;
            }
        case Flow:
            switch (serverity) {
            case Critical:
                if (value >= upThresholdError) {
                    return AlarmCauseEnum.FLOW_HIGH_CRITICAL;
                } else {
                    return AlarmCauseEnum.FLOW_LOW_CRITICAL;
                }
            case Warn:
                if (value >= upThresholdWarning && value < upThresholdError) {
                    return AlarmCauseEnum.FLOW_HIGH_WARN;
                } else {
                    return AlarmCauseEnum.FLOW_LOW_WARN;
                }
            default:
                return AlarmCauseEnum.NONE;
            }
        case Level:
            switch (serverity) {
            case Critical:
                if (value >= upThresholdError) {
                    return AlarmCauseEnum.LEVEL_HIGH_CRITICAL;
                } else {
                    return AlarmCauseEnum.LEVEL_LOW_CRITICAL;
                }
            case Warn:
                if (value >= upThresholdWarning && value < upThresholdError) {
                    return AlarmCauseEnum.LEVEL_HIGH_WARN;
                } else {
                    return AlarmCauseEnum.LEVEL_LOW_WARN;
                }
            default:
                return AlarmCauseEnum.NONE;
            }
        default:
            return AlarmCauseEnum.NONE;
        }
    }

    public AlarmServerityEnum generateAlarmServerity(double value) {
        if (value <= downThresholdError) {
            return AlarmServerityEnum.Critical;
        } else if (value > downThresholdError && value <= downThresholdWarning) {
            return AlarmServerityEnum.Warn;
        } else if (value >= upThresholdError) {
            return AlarmServerityEnum.Critical;
        } else if (value >= upThresholdWarning && value < upThresholdError) {
            return AlarmServerityEnum.Warn;
        } else {
            // should not be alarm here.
            return AlarmServerityEnum.None;
        }
    }

    public String generateAlarmDescription(Alarm alarm) {
        return alarm.getDescription();
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public StateEnum getState() {
        return state;
    }

    public void setState(StateEnum state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Sensor [station=" + station + ", type=" + type + ", value="
                + value + ", upThresholdWarning=" + upThresholdWarning
                + ", downThresholdWarning=" + downThresholdWarning
                + ", upThresholdError=" + upThresholdError
                + ", downThresholdError=" + downThresholdError + ", Id=" + Id
                + ", toString()=" + super.toString() + "]";
    }

    public static SensorTypeEnum getTypeByName(String typeName) {
        if (typeName.equalsIgnoreCase(SensorTypeEnum.PH.getType())) {
            return SensorTypeEnum.PH;
        } else if (typeName.equalsIgnoreCase(SensorTypeEnum.Temperature
                .getType())) {
            return SensorTypeEnum.Temperature;
        } else if (typeName.equalsIgnoreCase(SensorTypeEnum.Flow.getType())) {
            return SensorTypeEnum.Flow;
        } else if (typeName.equalsIgnoreCase(SensorTypeEnum.Level.getType())) {
            return SensorTypeEnum.Level;
        } else {
            return SensorTypeEnum.None;
        }
    }

    public static enum SensorTypeEnum {
        PH("ph"), Temperature("temperature"), Flow("flow"), Level("level"), None(
                "none");

        private final String type;

        private SensorTypeEnum(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }

        public String getType() {
            return type;
        }
    }
}
