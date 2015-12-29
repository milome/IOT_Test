package com.iot.cisp.wt.websocket.client;


import com.datasift.client.DataSiftClient;
import com.datasift.client.DataSiftConfig;
import com.datasift.client.core.Stream;
import com.datasift.client.stream.DataSiftMessage;
import com.datasift.client.stream.DeletedInteraction;
import com.datasift.client.stream.ErrorListener;
import com.datasift.client.stream.Interaction;
import com.datasift.client.stream.MultiStreamInteraction;
import com.datasift.client.stream.StreamEventListener;
import com.datasift.client.stream.StreamSubscription;

import io.higgs.ws.client.WebSocketClient;
import io.higgs.ws.client.WebSocketEventListener;
import io.higgs.ws.client.WebSocketMessage;
import io.higgs.ws.client.WebSocketStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author hluan
 */
public class StreamingData implements WebSocketEventListener {
    protected URI endpoint;
    protected WebSocketStream liveStream;
    protected DataSiftConfig config;
    protected Map<Stream, StreamSubscription> subscriptions = new NonBlockingHashMap<>();
    protected ErrorListener errorListener;
    protected StreamEventListener streamEventListener;
    protected boolean connected;
    protected Set<StreamSubscription> unsentSubscriptions = new NonBlockingHashSet<>();
    protected short MAX_TIMEOUT = 320, currentTimeout = 1;
    protected DateTime lastSeen;
    protected static Set<StreamingData> streams = new NonBlockingHashSet<>();
    public static boolean detectDeadConnection = true;
    public static int CONNECTION_TIMEOUT_LIMIT = 65;
    public static int CONNECTION_TIMEOUT = 65;

    static {
        //DS produces some fairly big websocket frames. usually 1 or 2 MB max but set to 20 to be sure
        WebSocketClient.maxFramePayloadLength = 20971520; //20 MB
        new Thread(new Runnable() {
            public void run() {
                while (detectDeadConnection) {
                    long now = DateTime.now().getMillis();
                    for (StreamingData data : streams) {
                        if (data.lastSeen != null) {
                            if (now - data.lastSeen.getMillis() >=
                                    TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT_LIMIT)) {
//                                data.closeAndReconnect();
                            }
                        }
                    }
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT));
                    } catch (InterruptedException e) {
                        LoggerFactory.getLogger(getClass()).info("Interrupted while waiting to check conn");
                    }
                }
            }
        }).start();
    }

    protected Logger log = LoggerFactory.getLogger(getClass());

    public StreamingData(DataSiftConfig config) {
        try {
            endpoint = new URI("ws://localhost:8080/waterTreatment/websocket/gateway");
        } catch (URISyntaxException e) {
            log.error("Unable to create endpoint URL", e);
        }
        this.config = config;
        streams.add(this);
    }

    @Override
    public synchronized void onConnect(ChannelHandlerContext ctx) {
        streamEventListener.streamOpened();
        connected = true;
        System.out.println("******************************** Connected!");
  
        pushUnsentSubscriptions();
    }

    @Override
    public synchronized void onClose(ChannelHandlerContext ctx, CloseWebSocketFrame frame) {
        closeAndReconnect();
    }

    private void closeAndReconnect() {
        streamEventListener.streamClosed();
        synchronized (StreamingData.this) {
            connected = false;
        }
        if (config.isAutoReconnect() &&
                TimeUnit.SECONDS.toMillis(currentTimeout) <= TimeUnit.SECONDS.toMillis(MAX_TIMEOUT)) {
            currentTimeout *= 2;
        }
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(currentTimeout));
        } catch (InterruptedException ignored) {
            log.info("Sleep interrupted, reconnecting");
        }
        liveStream = null;
        //re-subscribe
        unsentSubscriptions.addAll(subscriptions.values());
        connect();
    }

    @Override
    public void onPing(ChannelHandlerContext ctx, PingWebSocketFrame frame) {
        //only on ping or message should we reset the timeout
        currentTimeout = 1;
        lastSeen = DateTime.now();
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketMessage msg) {
        currentTimeout = 1;
        lastSeen = DateTime.now();
        System.out.println("On Message ---> " + msg.data());
        JSONObject json = null;
        try{
         json = new JSONObject(msg.data());
//        if(json.getString("station_id") != null) {

//        }
        } catch(org.json.JSONException e) {
            log.error("Exception",e);
            if(json != null)
            log.info("Json : {}",json);
        }
//        try {
//            MultiStreamInteraction mi =
//                    DataSiftClient.MAPPER.readValue(msg.data(), MultiStreamInteraction.class);
//            if (mi.isDataSiftMessage()) {
//                fireMessage(new DataSiftMessage(mi));
//            } else {
//                if (mi.getData().get("deleted") != null) {
//                    streamEventListener.onDelete(new DeletedInteraction(mi));
//                } else {
//                    fireInteraction(mi.getHash(), new Interaction(mi.getData()));
//                }
//            }
//        } catch (IOException e) {
//            fireError(e);  //unlikely but possible
//        }
    }

    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause, FullHttpResponse response) {
        if (cause != null) {
            fireError(cause);
        }
    }

    protected synchronized void connect() {
        if (liveStream == null) {
            liveStream = WebSocketClient.connect(endpoint, false, new String[] {"ws"});
            liveStream.subscribe(this);
        }
    }

    protected void fireInteraction(String hash, Interaction interaction) {
        for (Map.Entry<Stream, StreamSubscription> e : subscriptions.entrySet()) {
            if (e.getKey().isSameAs(hash)) {
                e.getValue().onMessage(interaction);
            }
        }
    }

    protected void fireMessage(DataSiftMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message can't be null!");
        }
        for (Map.Entry<Stream, StreamSubscription> e : subscriptions.entrySet()) {
            if (message.hashHashes()) {
                //if we have hashes then only subscriptions for each hash should be notified
                for (Stream hash : message.hashes()) {
                    if (e.getKey().isSameAs(hash)) {
                        e.getValue().onDataSiftLogMessage(message);
                    }
                }
            } else {
                //otherwise let everyone know
                e.getValue().onDataSiftLogMessage(message);
            }
        }
    }

    protected void fireError(Throwable e) {
        if (e == null) {
            throw new IllegalArgumentException("Error can't be null!");
        }
        errorListener.exceptionCaught(e);
    }

    /**
     * Subscribes a callback to listen for exceptions that may occur during streaming.
     * When exceptions occur it is unlikely we'll know which stream/subscription caused the exception
     * so instead of notifying all stream subscribers of the same exception this provides a way to list
     * the error  just once
     *
     * @param listener an error callback
     */
    public StreamingData onError(ErrorListener listener) {
        this.errorListener = listener;
        return this;
    }

    public void onStreamEvent(StreamEventListener streamEventListener) {
        this.streamEventListener = streamEventListener;
    }

    public StreamingData subscribe(final StreamSubscription subscription) {
        if (errorListener == null) {
            throw new IllegalStateException("You must call listen before subscribing to streams otherwise you'll miss" +
                    " any exceptions that may occur");
        }
        if (streamEventListener == null) {
            throw new IllegalStateException("You must call onStreamEvent before subscribing to streams otherwise " +
                    "you'll miss delete messages, which you are required to handle");
        }
        connect();
        unsentSubscriptions.add(subscription);
        if (connected) {
            pushUnsentSubscriptions();
        }
        return this;
    }

    protected void pushUnsentSubscriptions() {
        for (final StreamSubscription subscription : unsentSubscriptions) {
            if (!connected || liveStream.channel() == null || !liveStream.channel().isActive()) {
                synchronized (this) {
                    connected = false;
                }
                break;
            }
            subscriptions.put(subscription.stream(), subscription);
            liveStream.send("{\"action\":\"subscribe\",\"hash\":\"" + subscription.stream().hash() + "\"}")
                    .addListener(new GenericFutureListener<Future<Void>>() {
                        public void operationComplete(Future<Void> future) throws Exception {
                            if (future.isSuccess()) {
                                unsentSubscriptions.remove(subscription);
                            } else {
                                fireError(future.cause());
                                subscribe(subscription);
                            }
                        }
                    });
        }
        if (!connected) {
            connect();
        }
    }

    public StreamingData unsubscribe(Stream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream can't be null");
        }
        if (stream.hash() == null || stream.hash().isEmpty()) {
            throw new IllegalArgumentException("Invalid stream subscription request, no hash available");
        }
        connect();
        liveStream.send(" { \"action\" : \"unsubscribe\" , \"hash\": \"" + stream.hash() + "\"}");
        subscriptions.remove(stream);
        for (StreamSubscription subscription : unsentSubscriptions) {
            if (stream.isSameAs(subscription.stream())) {
                unsentSubscriptions.remove(subscription);
            }
        }
        return this;
    }
    
 // Delete handler
    public static class DeleteHandler extends StreamEventListener {
        public void onDelete(DeletedInteraction di) {
            // You must delete the interaction to stay compliant
            System.out.println("DELETED:\n " + di);
        }
    }

    // Error handler
    //handle exceptions that can't necessarily be linked to a specific stream
    public static class ErrorHandler extends ErrorListener {
        public void exceptionCaught(Throwable t) {
            t.printStackTrace();
            System.out.println("Caught exception in livestream error handler.");
            // TODO: do something useful..!
        }
    }
    
    public static void main(String args[]) throws InterruptedException {
        StreamingData test = new StreamingData(null);
        test.onError(new ErrorHandler());
        test.onStreamEvent(new DeleteHandler());
        test.connect();
        Thread.sleep(5000);
        JSONObject json = new JSONObject();
        json.put("command_id", "1");
        json.put("description", "close the downstream sluice.");
        test.liveStream.send(json.toString());

        // wait 5 seconds for messages from websocket
        Thread.sleep(5000);
        JSONObject json1 = new JSONObject();
        json1.put("command_id", "2");
        json1.put("description", "reduce the downstream sluice.");
        test.liveStream.send(" { \"command_id\" : \"1\" , \"description\" : \"reduce the downstream sluice.\"");
//        test.liveStream.send("This is a chat room?");
//        test.liveStream.send("Yes, server just echo the message.");
//        test.liveStream.send("Alright!");
        while(true) {
            ;
        }
    }
}
