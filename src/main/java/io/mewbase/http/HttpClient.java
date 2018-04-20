package io.mewbase.http;

public class HttpClient {


    public static void main(String[] args ) throws InterruptedException {
       final String hostname  =  System.getProperty("mewbase.host", "127.0.0.1");
       final int port = Integer.parseInt(System.getProperty("mewbase.port", "8080"));

        HttpEventSink sink = new HttpEventSink(hostname,port);
        long num = sink.publishSync("channel1", "look out!");
        System.out.println(num);


        HttpEventSource source = new HttpEventSource(hostname,port);
        HttpEventSubscription subs = source.subscribe("channel1");

        Thread.sleep(10 * 1000);

        subs.close();
        sink.close();
    }






}
