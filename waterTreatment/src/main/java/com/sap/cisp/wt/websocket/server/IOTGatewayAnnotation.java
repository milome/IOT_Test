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
package com.sap.cisp.wt.websocket.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
//import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;
import com.rapplogic.xbee.util.ByteUtils;
import com.sap.cisp.wt.util.Utils;
import com.sap.cisp.wt.websocket.client.WebsocketClientEndpoint;
import com.sap.cisp.wt.websocket.config.Alarm;
import com.sap.cisp.wt.websocket.config.Sensor;
import com.sap.cisp.wt.websocket.config.Sluice;
import com.sap.cisp.wt.websocket.config.Station;
import com.sap.cisp.wt.websocket.config.Sluice.SluiceStateEnum;

@ServerEndpoint(value = "/websocket/gateway")
public class IOTGatewayAnnotation {
    private static Log log = LogFactory.getLog(IOTGatewayAnnotation.class);
    private static final String GUEST_PREFIX = "Client";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<IOTGatewayAnnotation> connections = new CopyOnWriteArraySet<>();
    public static final BlockingQueue<Alarm> queue = new LinkedBlockingQueue<Alarm>();
//    private final static SimpleDateFormat sdf = new SimpleDateFormat(
//            "yyyy-MM-dd HH:mm:ss");
    private final String nickname;
    private Session session;
    public static XBee xbee = new XBee();
    public static WebsocketClientEndpoint clientEndPoint = null;
    protected static ScheduledThreadPoolExecutor xbeeSchedulerService = new ScheduledThreadPoolExecutor(
            3);
    protected static ScheduledThreadPoolExecutor alarmSchedulerService = new ScheduledThreadPoolExecutor(
            10);
    // Key - source address; value - station list
    protected static final Map<String, List<Station>> stationMap = new ConcurrentHashMap<String, List<Station>>();
    protected static final Map<Station, List<Sensor>> sensorMap = new ConcurrentHashMap<Station, List<Sensor>>();
    protected static Map<String, String> uriMap = null;
    protected static final String REAL_STATION_ID = "12";
    static {
        log.info("Init the websocket server to collect sensor data.");
        init();
    }

    public static void init() {
        // initialize stations
        initConfig("/iot.xml");
        // initialize XBee
        initXBee();
        alarmSchedulerService.scheduleAtFixedRate(new StationAgent(false), 2L, 3L,
                TimeUnit.SECONDS);
        alarmSchedulerService.scheduleAtFixedRate(new StationAgent(true), 0L, 30L,
                TimeUnit.SECONDS);
        alarmSchedulerService.execute(new AlarmAgent());
    }

    public static void initXBee() {
        // replace with the com port of your receiving XBee
        // (typically your end device)
        // router
        String comStr = (System.getProperty("os.name").toLowerCase()
                .indexOf("linux") >= 0) ? "/dev/ttyUSB0" : "COM5";
        try {
            xbee.open(comStr, 9600);
        } catch (XBeeException e) {
            // TODO Auto-generated catch block
            log.error("Cannot initialize the XBee with COM port " + comStr
                    + ".", e);
        }
        log.info("**** Initialize the XBee successfully. ****");
        xbeeSchedulerService.execute(new XBeeReceiver());
    }

    public static void initWebSocketClient(String uri) {
        // For now , only use one client endpoint
        try {
            if (uri.isEmpty()) {
                throw new URISyntaxException("URI is empty.", "failed");
            }
            clientEndPoint = new WebsocketClientEndpoint(
                    new URI(uri));
            if(clientEndPoint != null) {
                log.info("!!! Connect to the websocket server successfully.");
            } else {
                log.error("!!! Websocket server is not ready. Please try it later.");
                return;
            }
            clientEndPoint
                    .addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
                        public void handleMessage(String message) {
                            log.info("##### From server" + message);
                            JSONObject json = null;
                            try {
                                json = new JSONObject(message);
                                if (json.getString("station_id") != null) {
                                   log.info("##### Requested station id : " + json.getString("station_id"));
                                } else if (json.getString("command_id") != null) {
                                   log.info("##### Requested command id : " + json.getString("command_id"));
                                }
                            } catch (org.json.JSONException e) {
                                log.error(e);
                                if (json != null)
                                    log.info(json);
                            }
                        }
                    });
        } catch (URISyntaxException ue) {
            // TODO Auto-generated catch block
            log.error("Cannot initialize the websocket client with URI: "
                    + uri, ue);
        } catch(Exception e) {
            log.error("Cannot initialize the websocket client with URI: "
                    + uri, e);
        }
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

    public static void sendXBeeMessage(XBeeAddress64 destAddress, int[] payload) throws XBeeException {
        ZNetSenderHelper(xbee, destAddress, payload);
    }
    
    @SuppressWarnings("deprecation")
    private static void ZNetSenderHelper(XBee xbee, XBeeAddress64 destAddress, int[] payload)
            throws XBeeException {

        // coord (21A7)
        // XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40,
        // 0x8b, 0x98, 0xfe);

        // replace with end device's 64-bit address (SH + SL)
        // router (firmware 23A7)
        if(destAddress == null) {
            destAddress = new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40, 0xC8,
                    0xC9, 0x55);
        }
   

        // first request we just send 64-bit address. we get 16-bit network
        // address with status response
        ZNetTxRequest request = new ZNetTxRequest(destAddress, payload);

        log.debug(">> XBee Sender: zb request is " + Utils.printPayLoad(payload));

        try {
//            xbee.sendAsynchronous(request);
            xbee.sendSynchronous(request);
//            XBeeResponse response = xbee.getResponse();
//
//            log.info("received response " + response);
            int retryTimes = 1;
            while (retryTimes < 10) {
                log.info(">> XBee Sender: request packet bytes (base 16) " + ByteUtils.toBase16(request.getXBeePacket().getPacket()));
                
                long start = System.currentTimeMillis();
                log.info(">> XBee Sender: sending tx packet: " + request.toString() + " . Send for No. " + retryTimes + " time.");
                try {
                    ZNetTxStatusResponse response = (ZNetTxStatusResponse) xbee.sendSynchronous(request, 10000);
                    // update frame id for next request
                    request.setFrameId(xbee.getNextFrameId());
                    
                    log.info(">> XBee Sender: received response " + response);
                    
                    //log.debug("status response bytes:" + ByteUtils.toBase16(response.getPacketBytes()));
                    // I get the following message: Response in 75, Delivery status is SUCCESS, 16-bit address is 0x08 0xe5, retry count is 0, discovery status is SUCCESS 
                    log.info(">> XBee Sender: Response in " + (System.currentTimeMillis() - start) + ", Delivery status is " + response.getDeliveryStatus() + ", 16-bit address is " + ByteUtils.toBase16(response.getRemoteAddress16().getAddress()) + ", retry count is " +  response.getRetryCount() + ", discovery status is " + response.getDeliveryStatus());                 
                    if (response.getDeliveryStatus() == ZNetTxStatusResponse.DeliveryStatus.SUCCESS) {
                        // the packet was successfully delivered
                        if (response.getRemoteAddress16().equals(XBeeAddress16.ZNET_BROADCAST)) {
                            // specify 16-bit address for faster routing?.. really only need to do this when it changes
                            request.setDestAddr16(response.getRemoteAddress16());
                        }     
                        break;
                    } else {
                        // packet failed.  log error
                        // it's easy to create this error by unplugging/powering off your remote xbee.  when doing so I get: packet failed due to error: ADDRESS_NOT_FOUND  
                        log.error(">> XBee Sender: packet failed due to error: " + response.getDeliveryStatus());
                    }  
                } catch (XBeeTimeoutException e) {
                    log.warn(">> XBee Sender: request timed out");
                }
                try {
                    // wait a bit then send another packet
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.warn(">> XBee Sender: InterruptedException when sleep.");
                }
                retryTimes++;
            }

        } catch (XBeeTimeoutException e) {
            log.error("!!! Error, XBee request timed out");
            log.error(e);
        } catch(Exception e) {
            log.error("!!! Error during send the XBee request.");
            log.error(e);
        }
    }

    // }

    public IOTGatewayAnnotation() {
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

    @OnMessage
    public void incoming(String message) {
        JSONObject json = null;
        try {
            json = new JSONObject(message);
            if (json.getString("command_id") != null) {
                log.debug("Receive the command with id "
                        + json.getString("command_id")
                        + " ; Original message : " + message);
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
        log.error("Websocket Error: " + t.toString(), t);
    }

    public static void broadcast(String msg) {
        // Send to all incoming connections, as server
        for (IOTGatewayAnnotation client : connections) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
                log.debug("Websocket Error: Failed to send message to client",
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
    }

    private static class XBeeReceiver implements Runnable {
        public XBeeReceiver() {
        }

        public void run() {
            try {
                while (true) {
                    try {
                        // we wait here until a packet is received.
                        XBeeResponse response = xbee.getResponse();
                        log.info("************** received response "
                                + response.toString());
                        if (response.getApiId() == ApiId.ZNET_RX_RESPONSE) {
                            // we received a packet from ZNetSenderTest.java
                            if (response instanceof com.rapplogic.xbee.api.ErrorResponse) {
                                log.error("###### Received error response! "
                                        + response.toString());
                                break;
                            }
                            ZNetRxResponse rx = (ZNetRxResponse) response;
                            String sourceAddress = ByteUtils.toBase16(rx
                                    .getRemoteAddress64().getAddress());
                            log.info("Received RX packet, option is "
                                    + rx.getOption()
                                    + ", sender 64 address is "
                                    + sourceAddress
                                    + ", remote 16-bit address is "
                                    + ByteUtils.toBase16(rx
                                            .getRemoteAddress16().getAddress())
                                    + ", data is "
                                    + ByteUtils.toBase16(rx.getData()));
                            // Get the station from map via source address
                            Station station = null;
                            if (stationMap.get(sourceAddress) != null
                                    && !stationMap.get(sourceAddress).isEmpty()) {
                                station = stationMap.get(sourceAddress).get(0);
                            } else {
                                continue;
                            }
                            // log.info(">>>>>>>> Get the station: " +
                            // station.getSensorMap());
                            int data[] = rx.getData();
                            log.info("************* source address: "
                                    + sourceAddress + " ****************");
                            log.info("===>PH  H: " + (data[0] << 8) + " L: "
                                    + data[1] + " Total: "
                                    + ((data[0] << 8) + data[1]));
                            log.info("===>Temperature sensor H: "
                                    + (data[2] << 8) + " L: " + data[3]
                                    + " Total: " + ((data[2] << 8) + data[3]));
                            log.info("===>Water flow H: " + (data[4] << 8)
                                    + " L: " + data[5] + " Total: "
                                    + ((data[4] << 8) + data[5]));
                            log.info("===>Water Level H: " + (data[6] << 8)
                                    + " L: " + data[7] + " Total: "
                                    + ((data[6] << 8) + data[7]));
                            log.info("===>Light H: " + (data[8] << 8)
                                    + " L: " + data[9] + " Total: "
                                    + ((data[8] << 8) + data[9]));
                            double ph = Double.parseDouble(Utils.format(Double
                                    .parseDouble(((data[0] << 8) + data[1])
                                            / 100.0 + "")));
                            double temperature = Double.parseDouble(Utils.format(Double
                                    .parseDouble(((data[2] << 8) + data[3]) / 100.0 + "")));
                            double flow = Double.parseDouble(Utils.format(Double.parseDouble((data[4] << 8)
                                    + data[5] + "")));
                            double level = Double.parseDouble(Utils.format(Double
                                    .parseDouble(((data[6] << 8) + data[7])
                                            / 600.0 * 4 + "")));
                            double light = Double.parseDouble(Utils.format(Double.parseDouble((data[8] << 8)
                                    + data[9] + "")));
                            //For test purpose, no temperature sensor on station 12.
                            if(station.getId().equalsIgnoreCase(Integer.toString(IOTSensorDataAnnotation.demoSourceStationId))) {
                                temperature = Utils.simulateTemperatureByLight(light);
                                ph = Utils.simulatePHByLight(light);
                            }
     
                            int stationId = Integer.parseInt(station.getId());
                            int phSensorId = (stationId - 1) * 4 + 1;
                            int temperatureSensorId = (stationId - 1) * 4 + 2;
                            int flowSensorId = (stationId - 1) * 4 + 3;
                            int levelSensorId = (stationId - 1) * 4 + 4;
                            log.info("Station ID: " + stationId
                                    + "; PH sensor ID: " + phSensorId
                                    + "; Temperature sensor ID: "
                                    + temperatureSensorId
                                    + "; Flow sensor ID: " + flowSensorId
                                    + "; Level Sensor ID: " + levelSensorId);
                            station.getSensorMap()
                                    .get(Integer.toString(phSensorId))
                                    .setValue(ph);
                            station.getSensorMap()
                                    .get(Integer.toString(temperatureSensorId))
                                    .setValue(temperature);
                            station.getSensorMap()
                                    .get(Integer.toString(flowSensorId))
                                    .setValue(flow);
                            station.getSensorMap()
                                    .get(Integer.toString(levelSensorId))
                                    .setValue(level);

                            JSONObject json = new JSONObject();
                            json.put("version", "v0.1");
                            json.put("timestamp", "");
//                                    sdf.format(Calendar
//                                    .getInstance().getTime()));
                            JSONArray jsonArray = new JSONArray();
                            // For UI
                            JSONObject jsonUI = new JSONObject();
                            JSONArray jsonUIArray = new JSONArray();
                            JSONObject waterJson = new JSONObject();
                            waterJson.put("id", levelSensorId);
                            waterJson.put(
                                    "type",
                                    station.getSensorMap()
                                            .get(Integer
                                                    .toString(levelSensorId))
                                            .getType().getType());
                            waterJson.put("latitude", station.getLatitude());
                            waterJson.put("longtitude", station.getLongitude());
                            // Tested: 600 -> 4cm
                            waterJson.put("current_value", level);
                            waterJson.put("unit", "cm");
                            waterJson.put("source_address", sourceAddress);
                            // for UI
                            JSONObject waterUIJson = new JSONObject();
                            waterUIJson.put("sensor_id", levelSensorId);
                            waterUIJson.put(
                                    "type",
                                    station.getSensorMap()
                                            .get(Integer
                                                    .toString(levelSensorId))
                                            .getType().getType());
                            waterUIJson.put("value", level);

                            JSONObject temperatureJson = new JSONObject();
                            temperatureJson.put("id", temperatureSensorId);
                            temperatureJson.put("type", station.getSensorMap()
                                    .get(Integer.toString(temperatureSensorId))
                                    .getType().getType());
                            temperatureJson.put("latitude",
                                    station.getLatitude());
                            temperatureJson.put("longtitude",
                                    station.getLongitude());
                            temperatureJson.put("current_value", temperature);
                            temperatureJson.put("unit", "°C");
                            temperatureJson.put("source_address", sourceAddress);
                            // for UI
                            JSONObject temperatureUIJson = new JSONObject();
                            temperatureUIJson.put("sensor_id",
                                    temperatureSensorId);
                            temperatureUIJson
                                    .put("type",
                                            station.getSensorMap()
                                                    .get(Integer
                                                            .toString(temperatureSensorId))
                                                    .getType().getType());
                            temperatureUIJson.put("value", temperature);

                            JSONObject hallJson = new JSONObject();
                            hallJson.put("id", flowSensorId);
                            hallJson.put(
                                    "type",
                                    station.getSensorMap()
                                            .get(Integer.toString(flowSensorId))
                                            .getType().getType());
                            hallJson.put("latitude", station.getLatitude());
                            hallJson.put("longtitude", station.getLongitude());
                            hallJson.put("current_value", flow);
                            hallJson.put("unit", "l/h");
                            hallJson.put("source_address", sourceAddress);
                            // for UI
                            JSONObject hallUIJson = new JSONObject();
                            hallUIJson.put("sensor_id", flowSensorId);
                            hallUIJson
                                    .put("type",
                                            station.getSensorMap()
                                                    .get(Integer
                                                            .toString(flowSensorId))
                                                    .getType().getType());
                            hallUIJson.put("value", flow);

                            JSONObject phJson = new JSONObject();
                            phJson.put("id", phSensorId);
                            phJson.put(
                                    "type",
                                    station.getSensorMap()
                                            .get(Integer.toString(phSensorId))
                                            .getType().getType());
                            phJson.put("latitude", station.getLatitude());
                            phJson.put("longtitude", station.getLongitude());
                            phJson.put("current_value", ph);
                            phJson.put("unit", "mol/l");
                            phJson.put("source_address", sourceAddress);
                            // for UI
                            JSONObject phUIJson = new JSONObject();
                            phUIJson.put("sensor_id", phSensorId);
                            phUIJson.put(
                                    "type",
                                    station.getSensorMap()
                                            .get(Integer.toString(phSensorId))
                                            .getType().getType());
                            phUIJson.put("value", ph);

                            jsonArray.put(phJson);
                            jsonArray.put(temperatureJson);
                            jsonArray.put(hallJson);
                            jsonArray.put(waterJson);
                            json.put("datastreams", jsonArray);

//                            broadcast(json.toString());

                            jsonUIArray.put(phUIJson);
                            jsonUIArray.put(temperatureUIJson);
                            jsonUIArray.put(hallUIJson);
                            jsonUIArray.put(waterUIJson);

                            jsonUI.put("station_id", stationId + "");
                            // Consider to mark the time stamp on server?
                            jsonUI.put("timestamp", "");
                            jsonUI.put("data", jsonUIArray);
                            IOTSensorDataAnnotation
                                    .broadcast(stationId, jsonUI.toString());
                            broadcast(jsonUI.toString());
                            
                            //only generate mock data once for every period
                            if(station.getId().equalsIgnoreCase(REAL_STATION_ID)) {
                                generateDummyData();
                            }

                        } else {
                            log.debug("received unexpected packet "
                                    + response.toString());
                        }
                    } catch (Exception e) {
                        log.error("Exception caught.", e);
                        break;
                    }
                }
            } finally {
                if (xbee != null && xbee.isConnected()) {
                    log.error("##### Non-recoverable error occured. Close XBee and exit");
                    xbee.close();
                }
            }
        }
                
        public void generateDummyData() {
            List<Station> dummyStations = stationMap.get("dummy");
            for(Station dummy:dummyStations) {
                int stationIdDummy = Integer.parseInt(dummy.getId());
                int phSensorIdDummy = (stationIdDummy - 1) * 4 + 1;
                int temperatureSensorIdDummy = (stationIdDummy - 1) * 4 + 2;
                int flowSensorIdDummy = (stationIdDummy - 1) * 4 + 3;
                int levelSensorIdDummy = (stationIdDummy - 1) * 4 + 4;
               
                /* Random dummy value to avoid alarms */
                double phDummy = Double.parseDouble(Utils.format(ThreadLocalRandom.current().nextDouble(6.96, 7.06)));
                double temperatureDummy = Double.parseDouble(Utils.format(ThreadLocalRandom.current().nextDouble(25.0, 25.8)));
                double flowDummy = Double.parseDouble(Utils.format(ThreadLocalRandom.current().nextDouble(30.0, 33.6)));
                double levelDummy = Double.parseDouble(Utils.format(ThreadLocalRandom.current().nextDouble(3.0, 3.3)));
                // For fake PH warn value, just keep it until alarm get cleared
                if (StationAgent.isResetNeeded()
                        && (stationIdDummy == IOTSensorDataAnnotation.demoSourceStationId - 1)) {
                    log.info("*** There is Critical alarm on demo source station. Do not set dummy PH value to the fake station.");
                } else {
                    dummy.getSensorMap().get(Integer.toString(phSensorIdDummy))
                            .setValue(phDummy);
                }
                dummy.getSensorMap()
                .get(Integer.toString(temperatureSensorIdDummy))
                .setValue(temperatureDummy);
                dummy.getSensorMap()
                .get(Integer.toString(flowSensorIdDummy))
                .setValue(flowDummy);
                dummy.getSensorMap()
                .get(Integer.toString(levelSensorIdDummy))
                .setValue(levelDummy);

                JSONObject jsonDummy = new JSONObject();
                jsonDummy.put("version", "v0.1");
                jsonDummy.put("timestamp", "");
                JSONArray jsonArrayDummy = new JSONArray();
                // For UI
                JSONObject jsonUIDummy = new JSONObject();
                JSONArray jsonUIArrayDummy = new JSONArray();
                //dummy level sensor
                JSONObject waterJsonDummy = new JSONObject();
                waterJsonDummy.put("id", levelSensorIdDummy);
                waterJsonDummy.put(
                        "type",
                        dummy.getSensorMap()
                                .get(Integer
                                        .toString(levelSensorIdDummy))
                                .getType().getType());
                waterJsonDummy.put("latitude", dummy.getLatitude());
                waterJsonDummy.put("longtitude", dummy.getLongitude());
                // Tested: 600 -> 4cm
                waterJsonDummy.put("current_value", levelDummy);
                waterJsonDummy.put("unit", "cm");
                waterJsonDummy.put("source_address", "dummy");
                // for UI
                JSONObject waterUIJsonDummy = new JSONObject();
                waterUIJsonDummy.put("sensor_id", levelSensorIdDummy);
                waterUIJsonDummy.put(
                        "type",
                        dummy.getSensorMap()
                                .get(Integer
                                        .toString(levelSensorIdDummy))
                                .getType().getType());
                waterUIJsonDummy.put("value", levelDummy);
                
                //dummy temperature sensor
                JSONObject temperatureJsonDummy = new JSONObject();
                temperatureJsonDummy.put("id", temperatureSensorIdDummy);
                temperatureJsonDummy.put("type", dummy.getSensorMap()
                        .get(Integer.toString(temperatureSensorIdDummy))
                        .getType().getType());
                temperatureJsonDummy.put("latitude",
                        dummy.getLatitude());
                temperatureJsonDummy.put("longtitude",
                        dummy.getLongitude());
                temperatureJsonDummy.put("current_value", temperatureDummy);
                temperatureJsonDummy.put("unit", "°C");
                temperatureJsonDummy.put("source_address", dummy);
                // for UI
                JSONObject temperatureUIJsonDummy = new JSONObject();
                temperatureUIJsonDummy.put("sensor_id",
                        temperatureSensorIdDummy);
                temperatureUIJsonDummy
                        .put("type",
                                dummy.getSensorMap()
                                        .get(Integer
                                                .toString(temperatureSensorIdDummy))
                                        .getType().getType());
                temperatureUIJsonDummy.put("value", temperatureDummy);

                //dummy flow sensor
                JSONObject hallJsonDummy = new JSONObject();
                hallJsonDummy.put("id", flowSensorIdDummy);
                hallJsonDummy.put(
                        "type",
                        dummy.getSensorMap()
                                .get(Integer.toString(flowSensorIdDummy))
                                .getType().getType());
                hallJsonDummy.put("latitude", dummy.getLatitude());
                hallJsonDummy.put("longtitude", dummy.getLongitude());
                hallJsonDummy.put("current_value", flowDummy);
                hallJsonDummy.put("unit", "l/h");
                hallJsonDummy.put("source_address", "dummy");
                // for UI
                JSONObject hallUIJsonDummy = new JSONObject();
                hallUIJsonDummy.put("sensor_id", flowSensorIdDummy);
                hallUIJsonDummy
                        .put("type",
                                dummy.getSensorMap()
                                        .get(Integer
                                                .toString(flowSensorIdDummy))
                                        .getType().getType());
                hallUIJsonDummy.put("value", flowDummy);

                //dummy PH sensor
                JSONObject phJsonDummy = new JSONObject();
                phJsonDummy.put("id", phSensorIdDummy);
                phJsonDummy.put(
                        "type",
                        dummy.getSensorMap()
                                .get(Integer.toString(phSensorIdDummy))
                                .getType().getType());
                phJsonDummy.put("latitude", dummy.getLatitude());
                phJsonDummy.put("longtitude", dummy.getLongitude());
                phJsonDummy.put("current_value", phDummy);
                phJsonDummy.put("unit", "mol/l");
                phJsonDummy.put("source_address", "dummy");
                // for UI
                JSONObject phUIJsonDummy = new JSONObject();
                phUIJsonDummy.put("sensor_id", phSensorIdDummy);
                phUIJsonDummy.put(
                        "type",
                        dummy.getSensorMap()
                                .get(Integer.toString(phSensorIdDummy))
                                .getType().getType());
                phUIJsonDummy.put("value", phDummy);

                jsonArrayDummy.put(phJsonDummy);
                jsonArrayDummy.put(temperatureJsonDummy);
                jsonArrayDummy.put(hallJsonDummy);
                jsonArrayDummy.put(waterJsonDummy);
                jsonDummy.put("datastreams", jsonArrayDummy);

//                broadcast(jsonDummy.toString());

                jsonUIArrayDummy.put(phUIJsonDummy);
                jsonUIArrayDummy.put(temperatureUIJsonDummy);
                jsonUIArrayDummy.put(hallUIJsonDummy);
                jsonUIArrayDummy.put(waterUIJsonDummy);

                jsonUIDummy.put("station_id", stationIdDummy + "");
                // Consider to mark the time stamp on server?
                jsonUIDummy.put("timestamp", "");
                jsonUIDummy.put("data", jsonUIArrayDummy);
                IOTSensorDataAnnotation
                        .broadcast(stationIdDummy, jsonUIDummy.toString());
                broadcast(jsonUIDummy.toString());
            }
        }
    }
}
