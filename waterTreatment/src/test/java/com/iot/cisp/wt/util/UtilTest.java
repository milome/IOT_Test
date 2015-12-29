package com.iot.cisp.wt.util;

import static org.junit.Assert.*;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iot.cisp.wt.util.Utils;
import com.iot.cisp.wt.websocket.server.IOTGatewayAnnotation;

public class UtilTest {
    private static Log log = LogFactory.getLog(UtilTest.class);

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoadXmlConfig() {
        String xml = "/iot.xml";
        IOTGatewayAnnotation.initConfig(xml);
    }

    @Test
    public void testJsonParse() {
        JSONObject json = null;
        String message = "{ \"station_id\" : 6 }";
        try {
            json = new JSONObject("{ \"station_id\" : 6 }");
            try{
            if(json.getJSONArray("data") != null ) {
                //ignore the possible loop sensor data message from server
                return;
            }
            } catch(JSONException e) {
                log.info("It's not a loop back message. go on.");
            }
            if (json.getInt("station_id") != 0 && json.length() == 1) {
                log.info("##### Requested station id : "
                        + json.getInt("station_id"));
                int requestedStationId = json.getInt("station_id");
                log.info("id : " + requestedStationId);
            }
        } catch (JSONException e) {
            log.error("Cannot convert the message to JSON Object."
                    + " Message: " + message);
        } finally {
            log.debug("Json Object: " + json);
        }
    }
    
    @Test
    public void testFormat() {
        double d1 = 6.93;
        assertEquals("6.9", Utils.format(d1));
        double d2 = 6.95;
        assertEquals("7.0", Utils.format(d2));
        double d3 = 100.35456465765656;
        assertEquals("100.4", Utils.format(d3));
    }
    
    @Test
    public void testLinearMappingTemperature() {
        for(int x =0; x < 451; x++) {
            double PH = Utils.simulateTemperatureByLight((double)(x*1.0));
            log.info("The mapping value : x=" + x + "; val=" + Utils.format(PH));
        }
    }
    
    @Test
    public void testLinerMappingPH() {
        for(int x =0; x < 451; x++) {
            double PH = Utils.simulatePHByLight((double)(x*1.0));
            log.info("The mapping value : x=" + x + "; val=" + Utils.format(PH));
        }
    }
    
    @Test
    public void testUtilsPrintPayLoad() {
        int[] payload = {'u','p','2'};
        assertEquals("{\'u\',\'p\',\'2\'}",Utils.printPayLoad(payload));
        int[] payload1 = {'i','n','j','e','c','t'};
        log.info(Utils.printPayLoad(payload1));
    }

}
