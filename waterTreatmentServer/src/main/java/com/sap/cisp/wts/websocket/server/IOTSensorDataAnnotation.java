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
package com.sap.cisp.wts.websocket.server;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
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
import org.json.JSONException;
import org.json.JSONObject;

@ServerEndpoint(value = "/websocket/server/sensordata")
public class IOTSensorDataAnnotation {
    private static Log log = LogFactory.getLog(IOTSensorDataAnnotation.class);
    private static final String GUEST_PREFIX = "Client";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<IOTSensorDataAnnotation> connections = new CopyOnWriteArraySet<>();
    private final String nickname;
    private Session session;
    protected ScheduledThreadPoolExecutor heartBeatSchedulerService = new ScheduledThreadPoolExecutor(
            2);
    
    static {
        init();
    }

    public static void init() {
        log.info("Init the websocket server to collect sensor data.");
    }

    public IOTSensorDataAnnotation() {
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
        heartBeatSchedulerService.scheduleAtFixedRate(new Runnable () {

            @Override
            public void run() {
                JSONObject hbJson =  new JSONObject();
                hbJson.put("type", "heartbeat");
                broadcast(hbJson.toString()); 
            }
   
        }, 10L, 30L, TimeUnit.SECONDS);
    }

    @OnOpen
    public void start(Session session) {
        this.session = session;
        connections.add(this);
        String message = String.format("* %s %s. Session id -> %s", nickname,
                "has joined.", session.getId());
        broadcast(session, message);
    }

    @OnClose
    public void end() {
        connections.remove(this);
        String message = String
                .format("* %s %s", nickname, "has disconnected.");
        broadcast(message);
    }

    /**
     * This method will be invoked when the <code>ServerEndpoint</code> receives
     * a message from client.
     * Message source:
     * 1- report from IOTRouter: sensor data/station state/sluice state/alarms
     * 2- request/command from UI
     * 
     * @param message
     *            The text message
     * @param userSession
     *            The session of the client
     */
    @OnMessage
    public void onMessage(String message, Session userSession) {
        log.info("##### Message from session id : " + userSession.getId()
                + ". " + message);
        JSONObject json = null;
        try {
            json = new JSONObject(message);
            if (json.getString("station_id") != null && json.length() == 1) {
                log.info("##### Requested station id : "
                        + json.getString("station_id"));
            }
        } catch (JSONException e) {
            log.error(e);
        } finally {
            broadcast(userSession, message);
        }
    }

    @OnError
    public void onError(Throwable t) throws Throwable {
        log.error("Websocket Error: " + t.toString(), t);
    }

    public static void broadcast(Session userSession, String msg) {
        for (IOTSensorDataAnnotation client : connections) {
            try {
                synchronized (client) {
                    // do not send back to itself
                    if (!client.session.getId().equalsIgnoreCase(
                            userSession.getId())) {
                        client.session.getBasicRemote().sendText(msg);
                    }
                }
            } catch (IOException e) {
                log.debug("Websocket Error: Failed to send message to client", e);
                connections.remove(client);
                try {
                    client.session.close();
                } catch (IOException e1) {
                    log.error(e1);
                }
                String message = String.format("* %s %s", client.nickname,
                        "has been disconnected.");
                log.error(message);
//                broadcast(message);
            }
        }
    }

    public static void broadcast(String msg) {
        for (IOTSensorDataAnnotation client : connections) {
            synchronized (client) {
                client.session.getAsyncRemote().sendText(msg);
            }
        }
    }
}
