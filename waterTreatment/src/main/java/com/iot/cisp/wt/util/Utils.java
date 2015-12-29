package com.iot.cisp.wt.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.iot.cisp.wt.websocket.config.Sensor;

public class Utils {
    private static Log logger  = LogFactory.getLog(Utils.class);
    private static InputStream inputStream = null;
    private static Document _document = null;
    public Utils(){
        
    }
    
    public static synchronized Document getDocument(String xmlFileName) {
        inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlFileName);
        try {
            logger.info("**************** Get the resource file: " +inputStream.available() + " path: " + inputStream.toString());
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            logger.error("Cannot read the input file.", e1);
        }
         _document = null;

        SAXReader reader = new SAXReader();
        try {
            _document = reader.read(inputStream);
        } catch (DocumentException e) {
            logger.error("[Data Source XML] xml parse DocumentException is error.");
            logger.error(e.getMessage());
            return null;
        }
        return _document;
    }

    public static synchronized Element getRootElement(String xmlFileName) {
        Document _document = getDocument(xmlFileName);
        if (null == _document) {
            logger.error("[Data Source XML] xml document is null.");
            return null;
        }
        Element rootElement = _document.getRootElement();
        return rootElement;
    }
    
    public static synchronized List<Map<String, Object>> getStationsConfig(
            String stationConfigXML) {
        List<Map<String, Object>> paramsList = new ArrayList<Map<String, Object>>();
        try{
        //iot
        Element rootElement = getRootElement(stationConfigXML);
        if (null == rootElement) {
            logger.error("[Station Config XML] root element is null.");
            return null;
        }

        @SuppressWarnings("unchecked")
        // stations
        Iterator<Element> iterator = ((Element)rootElement.elementIterator().next()).elementIterator();
        while (iterator.hasNext()) {
            Map<String, Object> params = new HashMap<String, Object>();
            Element ele = iterator.next();
            String id = ele.attributeValue("id");
            params.put("id", id);
            String latitude = ele.attributeValue("latitude");
            params.put("latitude", latitude);
            String longitude = ele.attributeValue("longitude");
            params.put("longitude", longitude);
            String sourceAddress = ele.attributeValue("sourceAddress");
            params.put("sourceAddress", sourceAddress);
           
            @SuppressWarnings("unchecked")
            Iterator<Element> it = ((Element)ele.elementIterator().next()).elementIterator("sensor");
            Map<String, Sensor> sensorMap = new HashMap<String, Sensor>();
            while (it.hasNext()) {
                Element el = it.next();
                Sensor sensor = new Sensor();
                String sensorId = el.attributeValue("id");
                sensor.setId(sensorId);
                String type = el.attributeValue("type");
                sensor.setType(Sensor.getTypeByName(type));
                String upThresholdWarning = el.attributeValue("upThresholdWarning");
                sensor.setUpThresholdWarning(Float.parseFloat(upThresholdWarning));
                String downThresholdWarning = el.attributeValue("downThresholdWarning");
                sensor.setDownThresholdWarning(Float.parseFloat(downThresholdWarning));
                String upThresholdError = el.attributeValue("upThresholdError");
                sensor.setUpThresholdError(Float.parseFloat(upThresholdError));
                String downThresholdError = el.attributeValue("downThresholdError");
                sensor.setDownThresholdError(Float.parseFloat(downThresholdError));
                sensorMap.put(sensorId, sensor);
            }
            params.put("sensors", sensorMap);
            paramsList.add(params);
        }
        }catch(Exception e) {
            logger.error(e);
        } finally {
            try {
                if(inputStream != null){
                    inputStream.close();
                }  
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return paramsList;
    }
    
    public static synchronized List<Map<String, String>> getSluiceConfig(
            String stationConfigXML) {
        List<Map<String, String>> paramsList = new ArrayList<Map<String, String>>();
        try{
        //iot
        Element rootElement = getRootElement(stationConfigXML);
        if (null == rootElement) {
            logger.error("[Station Config XML] root element is null.");
            return null;
        }

        @SuppressWarnings("unchecked")
        // stations
        Iterator<Element> iterator = ((Element)rootElement.elementIterator("sluices").next()).elementIterator();
        while (iterator.hasNext()) {
            Map<String, String> params = new HashMap<String, String>();
            Element ele = iterator.next();
            String id = ele.attributeValue("id");
            params.put("id", id);
            String latitude = ele.attributeValue("latitude");
            params.put("latitude", latitude);
            String longitude = ele.attributeValue("longitude");
            params.put("longitude", longitude);
            String sourceAddress = ele.attributeValue("sourceAddress");
            params.put("sourceAddress", sourceAddress);
            paramsList.add(params);
        }
        }catch(Exception e) {
            logger.error(e);
        } finally {
            try {
                if(inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return paramsList;
    }
    
    public static synchronized Map<String, String> getServerURI(
            String stationConfigXML) {
       Map<String, String> uriMap = new HashMap<String,String>();
        try{
        //iot
        Element rootElement = getRootElement(stationConfigXML);
        if (null == rootElement) {
            logger.error("[Station Config XML] root element is null.");
            return null;
        }

        @SuppressWarnings("unchecked")
        // servers
        Iterator<Element> iterator = ((Element)rootElement.elementIterator("servers").next()).elementIterator();
        while (iterator.hasNext()) {
            //server
            Element ele = iterator.next();
            String type = ele.attributeValue("type");
            String uri = ele.attributeValue("uri");
            uriMap.putIfAbsent(type, uri);
        }
        }catch(Exception e) {
            logger.error(e);
        } finally {
            try {
                if(inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return uriMap;
    }
    
    public static double simulateTemperatureByLight(double light) {
        double a = 30.0 / 450.0;
        double b = 10.0;
        double temp = (double)(a*light + b);
        if(temp > 27.0) {
            temp = 26.8;
        }
        logger.info("The mapping value : light=" + light + "; temperature=" + Utils.format(temp));
        return Double.parseDouble(Utils.format(temp));
    }
    
    public static double simulatePHByLight(double light) {
        double a = 5.0 / 135.0;
        double b = 2.0;
        double temp = (double)(a*light + b);
        if(temp > 10.5) {
            temp = 10.2;
        }
        logger.info("The mapping value : light=" + light + "; PH=" + Utils.format(temp));
        return Double.parseDouble(Utils.format(temp));
    }
    
    public static String format(double value) {
        DecimalFormat df = new DecimalFormat("0.0");  
        df.setRoundingMode(RoundingMode.HALF_UP);  
        return df.format(value);  
    }
    
    public static String printPayLoad(int[] payload) {
        StringBuffer buffer = new StringBuffer("{");
        for(int i : payload) {
            buffer.append("\'" +(char)i + "\',");
        }
        buffer.deleteCharAt(buffer.length()-1);
        buffer.append("}");
        return buffer.toString();
    }
    
    public static enum StateEnum {
        Green("green"), Yellow("orange"), Red("red");

        private final String state;

        private StateEnum(String state) {
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
}
