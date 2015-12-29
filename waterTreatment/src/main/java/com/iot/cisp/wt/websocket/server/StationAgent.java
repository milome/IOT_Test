package com.iot.cisp.wt.websocket.server;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONObject;

import com.iot.cisp.wt.util.Utils.StateEnum;
import com.iot.cisp.wt.websocket.config.Alarm;
import com.iot.cisp.wt.websocket.config.Sluice;
import com.iot.cisp.wt.websocket.config.Station;
import com.iot.cisp.wt.websocket.config.Alarm.AlarmCauseEnum;
import com.iot.cisp.wt.websocket.config.Alarm.AlarmServerityEnum;
import com.iot.cisp.wt.websocket.config.Alarm.AlarmStateEnum;
import com.iot.cisp.wt.websocket.config.Sluice.SluiceStateEnum;
import com.rapplogic.xbee.api.XBeeException;

public class StationAgent implements Runnable {
    public static final Map<String, Station> stationIdMap = new ConcurrentHashMap<String, Station>();
    public static final Map<String, Sluice> sluiceMap = new ConcurrentHashMap<String, Sluice>();
    private static Log log = LogFactory.getLog(StationAgent.class);
    private  boolean isAuditAgent = false;
    public static boolean isResetNeeded = false;

    public StationAgent(boolean isAuditAgent) {
        this.isAuditAgent = isAuditAgent;
    }

    @Override
    public void run() {
        // report station state regularly
        // url: /getMonitorsState
        // params: null
        // response: {“group_id”: 1, “state”: green/orange/red}
        printAlarmSet();
        log.info("*** Station Agent reporting ***");
        if(isAuditAgent) {
            log.info("### Audit report for station and sluice state. ###");
        }
        for (Entry<String, Station> entry : stationIdMap.entrySet()) {
            StateEnum stationState = checkStationState(entry.getKey());
            if (!isAuditAgent) {
                if (entry.getValue().getState().equals(stationState)) {
                    log.info(">>> Station : " + entry.getValue().getId()
                            + " ; State: " + entry.getValue().getState()
                            + ". No change.");
                    continue;
                }
            }
            entry.getValue().setState(stationState);
            JSONObject stationJson = new JSONObject();
            stationJson.put("station_id", entry.getKey());
            stationJson.put("state", stationState.getState());
            stationJson.put("type", "station");
            log.info("### Station agent report : " + stationJson.toString());
            IOTAlarmAgentAnnotation.broadcast(stationJson.toString());
        }

        // If sensor is PH sensor on destination station(6) and if
        // it's recovered from alarm, reset the valve via
        // XBee command
        // &&
        // If sensor is PH sensor on source station(12) and if
        // it's recovered from alarm, reset the valve via
        // XBee command
//        if (isResetNeeded()) {
//            int sourcePHSensorId = (IOTSensorDataAnnotation.demoSourceStationId - 1) * 4 + 1;
//            int destPHSensorId = (IOTSensorDataAnnotation.demoDestinationStationId - 1) * 4 + 1;
//            // If the two stations are green
//            if ((stationIdMap.get(Integer.toString(IOTSensorDataAnnotation.demoSourceStationId))
//                    .getState().equals(StateEnum.Green)
//                    && stationIdMap
//                            .get(Integer.toString(IOTSensorDataAnnotation.demoDestinationStationId))
//                            .getState().equals(StateEnum.Green) && StationAgent
//                        .isResetNeeded())
//            // Or the two PH sensors are green
//                    || (stationIdMap
//                            .get(Integer.toString(IOTSensorDataAnnotation.demoSourceStationId))
//                            .getSensorMap().get(Integer.toString(sourcePHSensorId)).getState()
//                            .equals(StateEnum.Green) && stationIdMap
//                            .get(Integer.toString(IOTSensorDataAnnotation.demoDestinationStationId))
//                            .getSensorMap().get(Integer.toString(destPHSensorId)).getState()
//                            .equals(StateEnum.Green))) {
//                try {
//                    log.info("*** Auto reset the sluices!!! ****");
//                    IOTAlarmAgentAnnotation.resetCommand();
//                    setResetNeeded(false);
//                } catch (XBeeException e) {
//                    log.error("*** Error occured during reset the valve by XBee. ***");
//                    log.error(e);
//                }
//            }
//        }
        // Check sluice state
        for (Entry<String, Sluice> entry : sluiceMap.entrySet()) {
            log.info(">>> Sluice : " + entry.getValue().getId() + " ; State: "
                    + entry.getValue().getState() + ".");
            // audit message to avoid miss the start up message.
            if (isAuditAgent) {
                JSONObject sluiceJson = new JSONObject();
                sluiceJson.put("sluice_id", entry.getValue().getId());
                sluiceJson.put("state", entry.getValue().getState());
                sluiceJson.put("type", "sluice");
                IOTAlarmAgentAnnotation.broadcast(sluiceJson.toString());
            }
        }
    }

    public static StateEnum checkStationState(String id) {
        synchronized (AlarmAgent.stationAlarmMap) {
            if (AlarmAgent.stationAlarmMap.get(id) == null) {
                return StateEnum.Green;
            } else {
                Set<Alarm> alarmSet = AlarmAgent.stationAlarmMap.get(id);
                if (alarmSet.isEmpty())
                    return StateEnum.Green;
                Iterator<Alarm> iter = alarmSet.iterator();
                StateEnum state = StateEnum.Green;
                while (iter.hasNext()) {
                    Alarm alarm = iter.next();
                    // Do not count the cleared alarms
                    if (alarm.getState().equals(AlarmStateEnum.Cleared))
                        continue;
                    if (alarm.getSeverity().equalsIgnoreCase(
                            AlarmServerityEnum.Critical.getServerity())
                            && alarm.getState().equals(AlarmStateEnum.Active)) {
                        state = StateEnum.Red;
                        break;
                    }
                    if (alarm.getSeverity().equalsIgnoreCase(
                            AlarmServerityEnum.Warn.getServerity())
                            && alarm.getState().equals(AlarmStateEnum.Active)) {
                        state = StateEnum.Yellow;
                    }
                }
                return state;
            }
        }
    }

    public static SluiceStateEnum checkSluiceState(String id) {
        if (sluiceMap.get(id) == null) {
            return SluiceStateEnum.NA;
        } else {
            return sluiceMap.get(id).getState();
        }
    }

    public static synchronized boolean isResetNeeded() {
        return isResetNeeded;
    }

    public static synchronized void setResetNeeded(boolean isResetNeeded) {
        StationAgent.isResetNeeded = isResetNeeded;
    }

    public static void printAlarmSet() {
        log.info("@@@ Print the alarm set!!!");
        synchronized (AlarmAgent.stationAlarmMap) {
            for (Entry<String, Station> entry : stationIdMap.entrySet()) {
                Set<Alarm> alarmSet = AlarmAgent.stationAlarmMap.get(entry
                        .getKey());
                if (alarmSet == null) {
                    log.info(">> No alarm for station : " + entry.getKey());
                    continue;
                }

                Iterator<Alarm> iter = alarmSet.iterator();
                while (iter.hasNext()) {
                    Alarm alarm = iter.next();
                    if(alarm.getCause().equals(AlarmCauseEnum.PH_HIGH_CRITICAL) || alarm.getCause().equals(AlarmCauseEnum.PH_LOW_CRITICAL)){
                        // Check if station 6 or 12 raised alarm(currently should
                        // only have PH alarm), mark the flag
                        if (entry
                                .getKey()
                                .equalsIgnoreCase(
                                        Integer.toString(IOTSensorDataAnnotation.demoSourceStationId))
                                || entry.getKey()
                                        .equalsIgnoreCase(
                                                Integer.toString(IOTSensorDataAnnotation.demoDestinationStationId))) {
                            // set the reset needed flag
                            log.info("*** Detected PH Critical alarm. Set the reset flag as TRUE. ***");
                            setResetNeeded(true);
                        }
                    }
                    log.info(alarm);
                }
            }
        }
    }

}
