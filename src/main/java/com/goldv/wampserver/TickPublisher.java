package com.goldv.wampserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.goldv.wampserver.server.PublicationEvent;
import org.goldv.wampserver.server.WAMPPublisher;
import org.goldv.wampserver.server.WAMPSubscriber;
import org.goldv.wampserver.server.WAMPSubscription;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by goldv on 7/8/2015.
 */
public class TickPublisher implements WAMPPublisher{

  private final String topic;
  private final ScheduledExecutorService scheduler;
  private final Random rdm = new Random();

  public TickPublisher(String topic, ScheduledExecutorService scheduler){
    this.topic = topic;
    this.scheduler = scheduler;
  }

  public String baseTopic(){
    return topic;
  }

  public void onSubscribe(WAMPSubscriber sub){
    System.out.println("subscription received"  + sub.topic() );

    WAMPSubscription subscription = sub.subscribed();

    long delay = (long)(500 + (1000 * rdm.nextDouble()) );
    scheduler.scheduleAtFixedRate(new SubscriptionPublisher(subscription), 0, delay, TimeUnit.MILLISECONDS);
  }

  public void onUnsubscribe(String topic){
    System.out.println("unsubscribe for " + topic);
  }

  public static class Tick{
    public final String symbol;
    public final double bid;
    public final double ask;
    public final double volume;

    public Tick(String symbol, double ask, double bid, double volume) {
      this.symbol = symbol;
      this.ask = ask;
      this.bid = bid;
      this.volume = volume;
    }
  }

  public static class SubscriptionPublisher implements Runnable{

    private final WAMPSubscription subscription;
    private final Random rdm = new Random();
    private final ObjectMapper mapper = new ObjectMapper();

    public SubscriptionPublisher(WAMPSubscription subscription) {
      this.subscription = subscription;
    }

    @Override
    public void run() {
      JsonNode json = mapper.valueToTree(generate(subscription.topic()));
      subscription.publish(new PublicationEvent(subscription.topic(), "tick", json));
    }

    private Tick generate(String symbol){
      return new Tick(symbol, rdm.nextDouble(), rdm.nextDouble(), rdm.nextDouble());
    }
  }
}
