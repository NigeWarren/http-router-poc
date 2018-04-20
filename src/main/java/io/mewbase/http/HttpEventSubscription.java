package io.mewbase.http;


import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpEventSubscription  {

    private final static Logger logger = LoggerFactory.getLogger(HttpEventSubscription.class);

    // We use an HTTP Client per subscription such that the client can be closed
    // and the server can then use the disconnect shutdown logic
    private final HttpClient client;


    public HttpEventSubscription(final HttpClient httpClient,
                                 final SubscriptionRequest subsRequest) {

        this.client = httpClient;


        client.post("/"+HttpEventSource.subscribeRoute, response  -> {
                response.bodyHandler(totalBuffer -> {
                        try {
                            // use the subscription UUID to set up a web socket for this
                            String subscriptionURI = totalBuffer.toString();
                            client.websocket("/"+subscriptionURI, new Handler<WebSocket>() {
                                @Override
                                public void handle(WebSocket websocket) {
                                    websocket.handler(data -> {
                                            System.out.println("Received " + data);
                                    });
                                }
                            });

//                                ws.frameHandler( frame -> {
//                                   System.out.println("BOOM " + frame.textData());
//                                });
//                            });
                        } catch (Exception exp) {
                            logger.error("Event delivery failed", exp);
                        }
                    });


                }
        ).end(subsRequest.toString());
    }



    public void close()  {
        client.close();
        // drain and stop the dispatcher.
        logger.info("Event subscription closed");
    }

}