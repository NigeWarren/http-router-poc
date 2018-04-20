package io.mewbase.http;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class HttpEventSink  {

    private final static Logger logger = LoggerFactory.getLogger(HttpEventSink.class);

    // route uri for publishing events
    public final static String publishRoute = "publish";


    private final long syncWriteTimeOut = 10;

    private final HttpClient client;



    public HttpEventSink(String hostname, int port) {

        HttpClientOptions options = new HttpClientOptions()
                    .setDefaultHost(hostname)
                    .setDefaultPort(port);
                    // TODO lookup and set various security / protocol options here
                    // TODO - replace with Java 10 SE Library for HttpClient when able
        client = Vertx.vertx().createHttpClient(options);

        logger.info("Created HTTP Event Sink for "+hostname+":"+port);
    }



    public Long publishSync(String channelName, String  event) {
        CompletableFuture<Long> fut = publishAsync(channelName,event);
        try {
            return fut.get(syncWriteTimeOut,TimeUnit.SECONDS);
        } catch (Exception exp) {
            logger.error("Error attempting publishSync event to HttpEventSink", exp);
            return -111111l;
        }
    }



    public CompletableFuture<Long> publishAsync(final String channelName, String event) {

        CompletableFuture<Long> fut = new CompletableFuture<>();
        client.post("/"+publishRoute, response ->
                response.bodyHandler(totalBuffer -> {
                    try {
                        final String eventNumberAsString = totalBuffer.toString();
                        final Long eventNumber = Long.valueOf(eventNumberAsString);
                        fut.complete(eventNumber);
                    } catch (Exception exp) {
                        fut.completeExceptionally(exp);
                    }
                })
        ).end(channelName + ":" + event);
        return fut;
    }

    public void close() {
       client.close();
    }


}
