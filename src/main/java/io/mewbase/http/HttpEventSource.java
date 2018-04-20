package io.mewbase.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletionException;


public class HttpEventSource
{
    private final static Logger logger = LoggerFactory.getLogger(HttpEventSource.class);


    public final static String subscribeRoute = "subscribe";

    private final HttpClientOptions options;
    private final Vertx vertx = Vertx.vertx();


    public HttpEventSource(final String hostname, final int port ) {

        options = new HttpClientOptions()
                .setDefaultHost(hostname)
                .setDefaultPort(port);
        // TODO lookup and set various security / protocol options here
        // TODO - replace with Java 10 SE Library for HttpClient when able
        logger.info("Created HTTP Event Source for "+hostname+":"+port);
    }


    public HttpEventSubscription subscribe(final String channelName) {
            try {
                final SubscriptionRequest.SubscriptionType subsType = SubscriptionRequest.SubscriptionType.FromNow;
                final SubscriptionRequest subsRq = new SubscriptionRequest(channelName, subsType,0L,Instant.EPOCH);
                return new HttpEventSubscription(vertx.createHttpClient(options),  subsRq );
            } catch (Exception exp) {
                throw new CompletionException(exp);
            }
        }


    public HttpEventSubscription subscribeFromMostRecent(final String channelName) {
        try {
            final SubscriptionRequest.SubscriptionType subsType = SubscriptionRequest.SubscriptionType.FromMostRecent;
            final SubscriptionRequest subsRq = new SubscriptionRequest(channelName, subsType,0L,Instant.EPOCH);
            return new HttpEventSubscription(vertx.createHttpClient(options),  subsRq );
        } catch (Exception exp) {
            throw new CompletionException(exp);
        }
    }


    public HttpEventSubscription subscribeFromEventNumber(String channelName, Long startInclusive) {
        try {
            final SubscriptionRequest.SubscriptionType subsType = SubscriptionRequest.SubscriptionType.FromEventNumber;
            final SubscriptionRequest subsRq = new SubscriptionRequest(channelName,subsType,startInclusive,Instant.EPOCH);
            return new HttpEventSubscription(vertx.createHttpClient(options),  subsRq );
        } catch (Exception exp) {
            throw new CompletionException(exp);
        }
    }


    public HttpEventSubscription subscribeFromInstant(String channelName, Instant startInstant) {
        try {
            final SubscriptionRequest.SubscriptionType subsType = SubscriptionRequest.SubscriptionType.FromInstant;
            final SubscriptionRequest subsRq = new SubscriptionRequest(channelName, subsType,0L,startInstant);
            return new HttpEventSubscription(vertx.createHttpClient(options),  subsRq );
        } catch (Exception exp) {
            throw new CompletionException(exp);
        }
    }


    public HttpEventSubscription subscribeAll(String channelName) {
        try {
            final SubscriptionRequest.SubscriptionType subsType = SubscriptionRequest.SubscriptionType.FromStart;
            final SubscriptionRequest subsRq = new SubscriptionRequest(channelName, subsType,0L,Instant.EPOCH);
            return new HttpEventSubscription(vertx.createHttpClient(options),  subsRq );
        } catch (Exception exp) {
            throw new CompletionException(exp);
        }
    }


    public void close() {
       // Todo - Shut down all streams
    }


}
