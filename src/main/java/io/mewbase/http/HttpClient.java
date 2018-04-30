package io.mewbase.http;

public class HttpClient {


    public static void main(String[] args ) throws InterruptedException {

        // can set these with -D on command line
       final String hostname  =  System.getProperty("mewbase.host", "127.0.0.1");
       final int port = Integer.parseInt(System.getProperty("mewbase.port", "8080"));

        HttpEventSink sink = new HttpEventSink(hostname,port);
        HttpEventSource source = new HttpEventSource(hostname,port);

        // Send a sync publish
        sink.publishSync("channel1", "Event 0");

        // Set up a subscription to channel 1
        HttpEventSubscription subs1 = source.subscribe("channel1");

        // interleave publish with open subscribe
        sink.publishSync("channel2", "Event 1");

        // Set up a subscription to channel 2
        HttpEventSubscription subs2 = source.subscribe("channel2");

        // wait for three seconds and close the subscription to channel 1
        Thread.sleep(3 * 1000);
        subs1.close();

        // Send async event to check vanilla HTTP connections still good.
        sink.publishAsync("channel2", "Event 3");

        // wait for three more seconds and close the subscription to channel 2
        Thread.sleep(3 * 1000);
        subs2.close();

        // close the publisher as well.
        sink.close();
    }
    
}
