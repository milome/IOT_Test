package com.sap.cisp.wt.websocket.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONObject;

import com.sap.cisp.wt.util.Utils;
import com.sap.cisp.wt.websocket.config.Alarm;
import com.sap.cisp.wt.websocket.config.Alarm.AlarmActionEnum;
import com.sap.cisp.wt.websocket.config.Alarm.AlarmStateEnum;

public class AlarmAgent implements Runnable {
    private static Log log = LogFactory.getLog(AlarmAgent.class);
    public static final Map<String, Set<Alarm>> stationAlarmMap = new ConcurrentHashMap<String, Set<Alarm>>();

    public AlarmAgent() {

    }

    /**
     * Raise an alarm
     * 
     * @param alarm
     * @return
     */
    public static boolean raiseAlarm(Alarm alarm) {
        String stationId = alarm.getStationId();
        synchronized (stationAlarmMap) {
            Set<Alarm> alarmSet = stationAlarmMap.get(stationId);
            if (alarmSet == null) {
                alarmSet = new ConcurrentSkipListSet<Alarm>();
                alarmSet.add(alarm);
                stationAlarmMap.putIfAbsent(stationId, alarmSet);
                alarm.setAction(AlarmActionEnum.Raise);
                alarm.setState(AlarmStateEnum.Active);
                addAlarmToQueue(alarm);
                return true;
            }
            // case 0: There is an active alarm with same severity on place.
            if (alarmSet.contains(alarm)) {
                for (Alarm alarmItem : alarmSet) {
                    // same alarm(id/severity/cause/desc etc.) which has been
                    // cleared before, re-raise it.
                    if (alarmItem.equals(alarm)) {
                        if (alarmItem.getState().equals(AlarmStateEnum.Cleared)) {
                            alarmItem.setAction(AlarmActionEnum.Raise);
                            alarmItem.setState(AlarmStateEnum.Active);
                            log.info("%%% Re-raise the previous alarm with same severity : "
                                    + alarm.getSeverity());
                            return true;
                        } else {
                            log.info("^^^ There is already an active alarm in place. Do nothing. Alarm: "
                                    + alarmItem);
                            return true;
                        }
                    }
                }
                return false;
            } else {
                String sensorId = alarm.getSensorId();
                for (Alarm alarmItem : alarmSet) {
                    // if there is alarm for the same sensor, remove it.
                    if (alarmItem.getSensorId().equalsIgnoreCase(sensorId)) {
                        // Clear the previous different alarm on the same sensor
                        log.info("*** Clear and remove the previous alarm."
                                + alarmItem);
                        if (alarmItem.getState().equals(AlarmStateEnum.Active)) {
                            alarmItem.setAction(AlarmActionEnum.Clear);
                            alarmItem.setState(AlarmStateEnum.Cleared);
                        }
                        alarmSet.remove(alarmItem);
                    }
                }

                // case 4: no equal sensor ID, totally a new alarm
                log.info("*** Raise a new alarm instead." + alarm);
                alarm.setAction(AlarmActionEnum.Raise);
                alarm.setState(AlarmStateEnum.Active);

                addAlarmToQueue(alarm);
                alarmSet.add(alarm);
                return true;
            }
        }
    }

    public static void addAlarmToQueue(Alarm alarm) {
        log.warn("!!!!! Add alarm to queue." + alarm);
        IOTAlarmAgentAnnotation.queue.add(alarm);
    }

    /**
     * Clear all the alarms on one sensor.
     * 
     * @param stationId
     * @param sensorId
     * @return
     */
    public static boolean clearSensorAlarm(String stationId, String sensorId) {
        synchronized (stationAlarmMap) {
            Set<Alarm> alarmSet = stationAlarmMap.get(stationId);
            if (alarmSet == null) {
                return false;
            }

            for (Alarm alarmItem : alarmSet) {
                if (alarmItem.getSensorId().equalsIgnoreCase(sensorId)) {
                    alarmItem.setAction(AlarmActionEnum.Clear);
                    alarmItem.setState(AlarmStateEnum.Cleared);
                    alarmSet.remove(alarmItem);
                }
            }

            log.info("Cleared the alarms on sensor ID: " + sensorId
                    + " successfully.");

            return true;
        }
    }

    /**
     * Clear a specified alarm which has been active.
     * 
     * @param alarm
     * @return
     */
    public static boolean clearAlarm(Alarm alarm) {
        synchronized (stationAlarmMap) {
            Set<Alarm> alarmSet = stationAlarmMap.get(alarm.getStationId());
            if (alarmSet == null) {
                log.error("The station alarm set is null! Cannot clear alarm.");
                return false;
            }
            for (Alarm alarmItem : alarmSet) {
                if (alarmItem.getSensorId().equalsIgnoreCase(
                        alarm.getSensorId())
                        && alarmItem.getSeverity().equalsIgnoreCase(
                                alarm.getSensorId())
                        && alarmItem.getCause().getCause() == alarm.getCause()
                                .getCause()
                        && alarmItem.getState().equals(AlarmStateEnum.Active)) {
                    alarmItem.setAction(AlarmActionEnum.Clear);
                    alarmItem.setState(AlarmStateEnum.Cleared);
                    alarmSet.remove(alarmItem);
                    return true;
                }
            }
            log.error("Cannot clear the specified alarm: " + alarm);
            return false;
        }
    }

    @Override
    public void run() {
        try {
            Alarm alarm = null;
            while (true) {
                try {
                    // Block until queue is not null
                    alarm = IOTAlarmAgentAnnotation.queue.take();
                    log.debug("Queue Size ==> "
                            + IOTAlarmAgentAnnotation.queue.size());
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                JSONObject json = new JSONObject();
                json.put("type", "alarm");
                // Consider to mark the timestamp on server, instead of
                // raspberry PI.
                json.put("timestamp", "");// System.currentTimeMillis());
                json.put("description", alarm.getDescription());
                json.put("station_id", alarm.getStationId());
                json.put("sensor_id", alarm.getSensorId());
                json.put("severity", alarm.getSeverity());
                json.put("state", alarm.getState());
                json.put("longitude",
                        StationAgent.stationIdMap.get(alarm.getStationId())
                                .getLongitude());
                json.put("latitude",
                        StationAgent.stationIdMap.get(alarm.getStationId())
                                .getLatitude());
                json.put("cause", alarm.getCause().getCause());
                json.put("value", alarm.getType().toString().toUpperCase()
                        + " " + Utils.format(alarm.getValue()));
                json.put("type", "alarm");
                log.info("$$$ Report alarm: " + json.toString());
                IOTAlarmAgentAnnotation.broadcast(json.toString());
            }
        } catch (Exception e) {
            log.error("Exception caught in Alarm Agent: " + e);
        } finally {
            log.error("!!!!! AlarmAgent thread exit!");
        }

    }
}
