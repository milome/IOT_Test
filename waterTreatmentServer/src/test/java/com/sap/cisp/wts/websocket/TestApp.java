package com.sap.cisp.wts.websocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONObject;

public class TestApp {
    private static Log log = LogFactory.getLog(TestApp.class);
    public static void main(String[] args) {
        try {
            // open websocket
            final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI("ws://localhost:8080/waterTreatmentServer/websocket/server/gateway"));
            final WebsocketClientEndpoint clientEndPoint1 = new WebsocketClientEndpoint(new URI("ws://localhost:8080/waterTreatmentServer/websocket/server/alarmagent"));
            // add listener
            clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
                public void handleMessage(String message) {
                    System.out.println("#####" + message);
                    JSONObject json = null;
                    try{
                     json = new JSONObject(message);
//                    if(json.getString("station_id") != null) {
                        System.out.println(message);
//                    }
                    } catch(org.json.JSONException e) {
                        log.error(e);
                        if(json != null)
                        log.info(json);
                    }
                }
            });

         // add listener
            clientEndPoint1.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
                public void handleMessage(String message) {
                    System.out.println("***** " + message);
                    JSONObject json = null;
                    try{
                     json = new JSONObject(message);
//                    if(json.getString("station_id") != null) {
                        System.out.println(message);
//                    }
                    } catch(org.json.JSONException e) {
                        log.error(e);
                        if(json != null)
                        log.info(json);
                    }
                }
            });
            
            // send message to websocket
//            {
//                "commandId": 1,
//                "description": "close the downstream sluice."
//              }
            JSONObject json = new JSONObject();
            json.put("command_id", "1");
            json.put("description", "close the downstream sluice.");
            clientEndPoint.sendMessage(json.toString());

            // wait 5 seconds for messages from websocket
            Thread.sleep(5000);
            JSONObject json1 = new JSONObject();
            json1.put("command_id", "2");
            json1.put("description", "reduce the downstream sluice.");
            clientEndPoint.sendMessage(json1.toString());
            while(true) {
                ;
            }

        } catch (InterruptedException ex) {
            System.err.println("InterruptedException exception: " + ex.getMessage());
        } catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
    }
}