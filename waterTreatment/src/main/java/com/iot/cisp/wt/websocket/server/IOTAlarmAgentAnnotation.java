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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;

@ServerEndpoint(value = "/websocket/alarmagent")
public class IOTAlarmAgentAnnotation {
    private static Log log = LogFactory.getLog(IOTAlarmAgentAnnotation.class);
    private static final String GUEST_PREFIX = "Client";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<IOTAlarmAgentAnnotation> connections = new CopyOnWriteArraySet<>();
    public static final BlockingQueue<Alarm> queue = new LinkedBlockingQueue<Alarm>();
    private final String nickname;
    private Session session;
    public static WebsocketClientEndpoint clientEndPoint = null;
    protected static final Map<String, List<Station>> stationMap = new ConcurrentHashMap<String, List<Station>>();
    protected static final Map<Station, List<Sensor>> sensorMap = new ConcurrentHashMap<Station, List<Sensor>>();
    protected static Map<String, String> uriMap = null;
    protected static ScheduledThreadPoolExecutor heartBeatSchedulerService = new ScheduledThreadPoolExecutor(
            3);

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
            if (sluice.getId().equalsIgnoreCase("3")) {
                // No. 3 is closed by default.
                sluice.setState(SluiceStateEnum.Closed);
            } else {
                sluice.setState(SluiceStateEnum.Open);
            }
            log.info("Sluice: " + sluice);
            StationAgent.sluiceMap.putIfAbsent(sluice.getId(), sluice);
        }
    }

    public IOTAlarmAgentAnnotation() {
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
        heartBeatSchedulerService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                JSONObject hbJson = new JSONObject();
                hbJson.put("type", "heartbeat");
                broadcast(hbJson.toString());
            }

        }, 10L, 30L, TimeUnit.SECONDS);
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
     * source: 1- command message(1/2/3) from UI->SERVER->IOTRouter 2- test
     * message from local UI 3- possible loop back alarm messages from server
     * 
     * @param message
     */
    @OnMessage
    public void incoming(String message) {
        JSONObject json = null;
        try {
            json = new JSONObject(message);
            try {
                if ((json.getString("type") != null && json.getString("type")
                        .equalsIgnoreCase("alarm"))
                        || (json.getString("type") != null && json.getString(
                                "type").equalsIgnoreCase("station"))
                        || (json.getString("type") != null && json.getString(
                                "type").equalsIgnoreCase("sluice"))) {
                    // ignore the possible loop back alarm message from server
                    return;
                }
            } catch (JSONException e) {
                log.info("It's not a loop back message. Go on.");
            }
            if (json.getInt("command") != 0 && json.length() == 1) {
                log.debug("Receive the command with id "
                        + json.getInt("command") + " ; Original message : "
                        + message);
                handleCommand(json.getInt("command"));
            }
        } catch (JSONException | XBeeException e) {
            log.error("Exception caught during processing incoming."
                    + " Message: " + message, e);
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
        log.error("Websocket Error: " + t.toString(), t);
    }

    /**
     * Obsolete method
     * 
     * @param uri
     */
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
//                            JSONObject json = null;
                            try {
                                final JSONObject json = new JSONObject(message);
                                if (json.getInt("command_id") != 0) {
                                    log.info("##### Requested command id : "
                                            + json.getInt("command_id"));
                                    heartBeatSchedulerService.execute(new Runnable(){
                                        @Override
                                        public void run() {
                                            try {
                                                handleCommand(json.getInt("command_id"));
                                            } catch (JSONException e) {
                                                // TODO Auto-generated catch block
                                                log.error(e);
                                            } catch (XBeeException e) {
                                                // TODO Auto-generated catch block
                                                log.error(e);
                                            }
                                            
                                        }                                  
                                    });                                
                                }
                            } catch (org.json.JSONException e) {
                                log.error(e);
                            }
                        }
                    });
        } catch (URISyntaxException ue) {
            // TODO Auto-generated catch block
            log.error(
                    "Cannot initialize the websocket client with URI: " + uri,
                    ue);
        } catch (Exception e) {
            log.error(
                    "Cannot initialize the websocket client with URI: " + uri,
                    e);
        }
    }

    /**
     * Send XBee command to board
     * !!! May need re-send???
     * @param command
     * @throws XBeeException
     */
    public static void handleCommand(int command) throws XBeeException {
        // source address 0x00,0x13,0xa2,0x00,0x40,0xa1,0x94,0xde, which connect
        // with relay shield
        XBeeAddress64 destAddressRelay = new XBeeAddress64(0, 0x13, 0xa2, 0,
                0x40, 0xa1, 0x94, 0xde);
        // source address 0x00,0x13,0xa2,0x00,0x40,0xc8,0xc9,0x55 which connect
        // with MEGA
        XBeeAddress64 destAddressMega = new XBeeAddress64(0x00, 0x13, 0xa2,
                0x00, 0x40, 0xc8, 0xc9, 0x55);

        switch (command) {
        case 1:
            // turn off No.2 sluice
            int[] payload1 = { 'o', 'f', 'f', '2' };
            log.info("*** Send the command: " + Utils.printPayLoad(payload1) + " to XBee.");
            StationAgent.sluiceMap.get("2").setState(SluiceStateEnum.Closed);
            IOTGatewayAnnotation.sendXBeeMessage(destAddressMega, payload1);
            break;
        case 2:
            // turn down No.4 sluice to 50%
            int[] payload2 = { 'd', 'o', 'w', 'n', '4' };
            log.info("*** Send the command: " + Utils.printPayLoad(payload2) + " to XBee.");
            StationAgent.sluiceMap.get("4").setState(SluiceStateEnum.SemiOpen);
            IOTGatewayAnnotation.sendXBeeMessage(destAddressRelay, payload2);
            break;
        case 3:
            // put the limestone
            int[] payload3 = { 'f', 'i', 'x' };
            log.info("*** Send the command: " + Utils.printPayLoad(payload3) + " to XBee.");
            IOTGatewayAnnotation.sendXBeeMessage(destAddressRelay, payload3);
            break;
        case 4:
            // reset sluice, for test purpose
            // Note: In this case, the two Ardunio boards must be online at the same time. Otherwise, XBeeException
            // will be caught and blocked in retry delay!
            log.info("*** Reset the sluice state for test purpose.");
            resetCommand();
            StationAgent.sluiceMap.get("4").setState(SluiceStateEnum.Open);
            StationAgent.sluiceMap.get("2").setState(SluiceStateEnum.Open);
            break;
        case 5:
            log.info("*** Inject the Acid liquid for testing.");
            int[] payload5 = {'i','n','j','e','c','t'};
            //more times?
            IOTGatewayAnnotation.sendXBeeMessage(destAddressRelay, payload5);
            break;
        case 6:
            log.info("*** Stop the Acid liquid injection for testing.");
            int[] payload6 = {'s','t','o','p','i','n'};
            IOTGatewayAnnotation.sendXBeeMessage(destAddressRelay, payload6);
            break;
        case 7:
            log.info("*** Reset the No.4 sluice state for test purpose.");
            resetCommand(4);
            StationAgent.sluiceMap.get("4").setState(SluiceStateEnum.Open);
            break;
        case 8:
            log.info("*** Reset the No.2 sluice state for test purpose.");
            resetCommand(2);
            StationAgent.sluiceMap.get("2").setState(SluiceStateEnum.Open);
            break;
        default:
            break;
        }
    }

    public static void resetCommand() throws XBeeException {
        // source address 0x00,0x13,0xa2,0x00,0x40,0xa1,0x94,0xde, which connect
        // with relay shield
        XBeeAddress64 destAddressRelay = new XBeeAddress64(0, 0x13, 0xa2, 0,
                0x40, 0xa1, 0x94, 0xde);
        // source address 0x00,0x13,0xa2,0x00,0x40,0xc8,0xc9,0x55 which connect
        // with MEGA
        XBeeAddress64 destAddressMega = new XBeeAddress64(0x00, 0x13, 0xa2,
                0x00, 0x40, 0xc8, 0xc9, 0x55);

        // turn on No.2 sluice
        int[] payload1 = { 'o', 'n', '2' };
        log.info("*** Reset No.2 sluice to open: " + payload1 + " via XBee.");
        StationAgent.sluiceMap.get("2").setState(SluiceStateEnum.Open);
        IOTGatewayAnnotation.sendXBeeMessage(destAddressMega, payload1);

        // turn up No.4 sluice to 100%
        int[] payload2 = { 'u', 'p', '4' };
        log.info("*** Turn up No.4 sluice to 100%: " + payload2 + " to XBee.");
        StationAgent.sluiceMap.get("4").setState(SluiceStateEnum.Open);
        IOTGatewayAnnotation.sendXBeeMessage(destAddressRelay, payload2);
    }
    
    public static void resetCommand(int index) throws XBeeException {
        // source address 0x00,0x13,0xa2,0x00,0x40,0xa1,0x94,0xde, which connect
        // with relay shield
        XBeeAddress64 destAddressRelay = new XBeeAddress64(0, 0x13, 0xa2, 0,
                0x40, 0xa1, 0x94, 0xde);
        // source address 0x00,0x13,0xa2,0x00,0x40,0xc8,0xc9,0x55 which connect
        // with MEGA
        XBeeAddress64 destAddressMega = new XBeeAddress64(0x00, 0x13, 0xa2,
                0x00, 0x40, 0xc8, 0xc9, 0x55);

        // turn on No.2 sluice
        if (index == 2) {
            int[] payload1 = { 'o', 'n', '2' };
            log.info("*** Reset No.2 sluice to open: " + payload1
                    + " via XBee.");
            StationAgent.sluiceMap.get("2").setState(SluiceStateEnum.Open);
            IOTGatewayAnnotation.sendXBeeMessage(destAddressMega, payload1);
        } else if (index == 4) {
            // turn up No.4 sluice to 100%
            int[] payload2 = { 'u', 'p', '4' };
            log.info("*** Turn up No.4 sluice to 100%: " + payload2
                    + " to XBee.");
            StationAgent.sluiceMap.get("4").setState(SluiceStateEnum.Open);
            IOTGatewayAnnotation.sendXBeeMessage(destAddressRelay, payload2);
        }
    }

    public static void broadcast(String msg) {
        log.info("**********alarm sending list*****************");
        for (IOTAlarmAgentAnnotation client : connections) {
            try {
                synchronized (client) {
                    log.info("client id : " + client.session.getId()
                            + " nickname: " + client.nickname
                            + " open session size: "
                            + client.session.getOpenSessions().size());
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
                log.debug(
                        "Communication Error: Failed to send message to client",
                        e);
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
        log.info("**********alarm sending list end**************");
    }
}
