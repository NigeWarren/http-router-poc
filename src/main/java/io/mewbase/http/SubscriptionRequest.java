package io.mewbase.http;



import java.time.Instant;

public class SubscriptionRequest {

    private static final String CHANNEL_TAG = "channel";
    private static final String SUBS_TYPE_TAG = "type";
    private static final String START_EVENT_TAG = "startEvent";
    private static final String START_INSTANT_TAG = "startInstant";


    public enum SubscriptionType {
        FromNow,
        FromMostRecent,
        FromEventNumber,
        FromInstant,
        FromStart
    }

    public final String channel;
    public final SubscriptionType type;
    public final Long startInclusive;
    public final Instant startInstant;


    public SubscriptionRequest(String channel, SubscriptionType type, Long startInclusive , Instant startInstant ) {
        this.channel = channel;
        this.type = type;
        this.startInclusive = startInclusive;
        this.startInstant = startInstant;
    }



    @Override
    public String toString() {
        return  " Channel :" + channel + " Type :" + type.name();
    }
}


