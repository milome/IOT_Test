/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.iot.cisp.wt.websocket.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
//import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iot.cisp.wt.util.Utils;
import com.iot.cisp.wt.websocket.client.WebsocketClientEndpoint;
import com.iot.cisp.wt.websocket.config.Alarm;
import com.iot.cisp.wt.websocket.config.Sensor;
import com.iot.cisp.wt.websocket.config.Sluice;
import com.iot.cisp.wt.websocket.config.Station;
import com.iot.cisp.wt.websocket.config.Sluice.SluiceStateEnum;

@ServerEndpoint(value = "/websocket/sensordata")
public class IOTSensorDataAnnotation {
    private static Log log = LogFactory.getLog(IOTSensorDataAnnotation.class);
    private static final String GUEST_PREFIX = "Client";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<IOTSensorDataAnnotation> connections = new CopyOnWriteArraySet<>();
    public static final BlockingQueue<Alarm> queue = new LinkedBlockingQueue<Alarm>();
    // private final static SimpleDateFormat sdf = new SimpleDateFormat(
    // "yyyy-MM-dd HH:mm:ss");
    private final String nickname;
    private Session session;
    private static int requestedStationId = 12;
    // Specified station for demo purpose
    public static final int demoSourceStationId = 12;
    public static final int demoDestinationStationId = 6;
    public static final double fakePHAcidWarnVal = 3.5;
    public static final double fakePHSodaWarnVal = 10.5;
    public static final int fakeAlarmDelay = 200;

    public static WebsocketClientEndpoint clientEndPoint = null;
    // 3);
    // Key - source address; value - station list
    protected static final Map<String, List<Station>> stationMap = new ConcurrentHashMap<String, List<Station>>();
    protected static final Map<Station, List<Sensor>> sensorMap = new ConcurrentHashMap<Station, List<Sensor>>();
    protected static Map<String, String> uriMap = null;

    static {
        log.info("Init the websocket server to collect sensor data.");
        init();
    }

    public static void init() {
        // initialize stations
        initConfig("/iot.xml");
    }

    @SuppressWarnings("unchecked")
    public static void initConfig(String xmlFile) {
        List<Map<String, Object>> paramsList = Utils.getStationsConfig(xmlFile);
        for (Map<String, Object> map : paramsList) {
            Station station = new Station();
            station.setId(map.get("id").toString());
            station.setLatitude(map.get("latitude").toString());
            station.setLongitude(map.get("longitude").toString());
            station.setSourceAddress(map.get("sourceAddress").toString());
            station.setDummy(station.getSourceAddress().equalsIgnoreCase(
                    "dummy") ? true : false);
            station.setState(Utils.StateEnum.Green);
            StationAgent.stationIdMap.putIfAbsent(station.getId(), station);
            log.info("Station: " + station);
            Map<String, Sensor> sensorMap = (Map<String, Sensor>) map
                    .get("sensors");
            for (Entry<String, Sensor> entry : sensorMap.entrySet()) {
                entry.getValue().setStation(station);
                log.info("Sensor : " + entry.getValue());
                station.getSensorMap().put(entry.getKey(), entry.getValue());
            }
            // station.setSensorMap(sensorMap);
            log.info("************** Print station: " + station.getSensorMap());
            if (stationMap.containsKey(station.getSourceAddress())) {
                stationMap.get(station.getSourceAddress()).add(station);
            } else {
                List<Station> stationList = new ArrayList<Station>();
                stationList.add(station);
                stationMap.putIfAbsent(station.getSourceAddress(), stationList);
            }
        }

        List<Map<String, String>> sluiceList = Utils.getSluiceConfig(xmlFile);
        for (Map<String, String> map : sluiceList) {
            Sluice sluice = new Sluice();
            sluice.setId(map.get("id"));
            sluice.setLatitude(map.get("latitude"));
            sluice.setLongitude(map.get("longitude"));
            sluice.setSourceAddress(map.get("sourceAddress"));
            if(sluice.getId().equalsIgnoreCase("3")) {
                //No. 3 is closed by default.
                sluice.setState(SluiceStateEnum.Closed);
            } else {
                sluice.setState(SluiceStateEnum.Open);
            }
            log.info("Sluice: " + sluice);
            StationAgent.sluiceMap.putIfAbsent(sluice.getId(), sluice);
        }
    }

    public static void initWebSocketClient(String uri) {
        // For now , only use one client endpoint
        try {
            if (uri.isEmpty()) {
                throw new URISyntaxException("URI is empty.", "failed");
            }
            clientEndPoint = new WebsocketClientEndpoint(new URI(uri));
            clientEndPoint
                    .addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
                        public void handleMessage(String message) {
                            log.info("##### From server" + message);
                            JSONObject json = null;
                            try {
                                json = new JSONObject(message);
                                if (json.getString("station_id") != null) {
                                    log.info("##### Requested station id : "
                                            + json.getString("station_id"));
                                    requestedStationId = Integer.parseInt(json
                                            .getString("station_id"));
                                }
                            } catch (org.json.JSONException e) {
                                log.error(e);
                                if (json != null)
                                    log.info(json);
                            }
                        }
                    });
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            log.error(
                    "Cannot initialize the websocket client with URI: " + uri,
                    e);
        }
    }

    /**
     * @return the requestedStationId
     */
    public static synchronized int getRequestedStationId() {
        return requestedStationId;
    }

    /**
     * @param requestedStationId
     *            the requestedStationId to set
     */
    public static synchronized void setRequestedStationId(int requestedStationId) {
        IOTSensorDataAnnotation.requestedStationId = requestedStationId;
    }

    public IOTSensorDataAnnotation() {
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
    }

    @OnOpen
    public void start(Session session) {
        this.session = session;
        connections.add(this);
        String message = String.format("* %s %s", nickname, "has joined.");
        log.info(message);
    }

    @OnClose
    public void end() {
        connections.remove(this);
        String message = String
                .format("* %s %s", nickname, "has disconnected.");
        log.info(message);
    }

    /**
     * Source: 1- station_id message from UI->SERVER->IOTRouter 2- test message
     * from local UI 3- possible loop back sensor data message
     * 
     * @param message
     */
    @OnMessage
    public void incoming(String message) {
        JSONObject json = null;
        try {
            json = new JSONObject(message);
            try {
                if (json.getJSONArray("data") != null) {
                    // ignore the possible loop sensor data message from server
                    return;
                }
            } catch (JSONException e) {
                log.info("It's not a loop back message. go on.");
            }
            if (json.getInt("station_id") != 0 && json.length() == 1) {
                log.info("##### Requested station id : "
                        + json.getInt("station_id"));
                requestedStationId = json.getInt("station_id");
            }
        } catch (JSONException e) {
            log.error("Cannot convert the message to JSON Object."
                    + " Message: " + message);
        } finally {
            log.debug("Json Object: " + json);
        }
    }

    /**
     * This method will be invoked when the <code>ServerEndpoint</code> receives
     * a message from client.
     * 
     * @param message
     *            The text message
     * @param userSession
     *            The session of the client
     */
    // @OnMessage
    // public void onMessage(String message, Session userSession) {
    // for (Session session : userSession.getOpenSessions()) {
    // if (session.isOpen())
    // session.getAsyncRemote().sendText(message);
    // }
    // }

    @OnError
    public void onError(Throwable t) throws Throwable {
        log.error("Communication Error: " + t.toString(), t);
    }

    public static void broadcast(String msg) {
        for (IOTSensorDataAnnotation client : connections) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
                log.debug("Communication Error: Failed to send message to client", e);
                connections.remove(client);
                try {
                    client.session.close();
                } catch (IOException e1) {
                    // Ignore
                }
                String message = String.format("* %s %s", client.nickname,
                        "has been disconnected.");
                log.error(message);
            }
        }
    }

    public static void broadcast(int stationId, String msg) {
        // only send the requested station data to UI.
        if (stationId == requestedStationId) {
            broadcast(msg);
        }
    }
}
