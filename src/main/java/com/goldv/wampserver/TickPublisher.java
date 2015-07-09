package com.goldv.wampserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.goldv.wampserver.server.PublicationEvent;
import org.goldv.wampserver.server.WAMPPublisher;
import org.goldv.wampserver.server.WAMPSubscriber;
import org.goldv.wampserver.server.WAMPSubscription;

import java.math.BigDecimal;
import java.util.List;
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
  private final List<String> symbols;

  public TickPublisher(String topic, ScheduledExecutorService scheduler, List<String> symbols){
    this.topic = topic;
    this.scheduler = scheduler;
    this.symbols = symbols;
  }

  public String topic(){
    return topic;
  }

  public void onSubscribe(WAMPSubscriber sub){
    System.out.println("subscription received"  + sub.topic() );

    WAMPSubscription subscription = sub.subscribed();

    for(String symbol : symbols){
      long delay = (long)(3000 + (5000 * rdm.nextDouble()) );
      scheduler.scheduleAtFixedRate(new SubscriptionPublisher(subscription, symbol), 0, delay, TimeUnit.MILLISECONDS);
    }
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
    private final String symbol;

    public SubscriptionPublisher(WAMPSubscription subscription, String symbol) {
      this.subscription = subscription;
      this.symbol = symbol;
    }

    @Override
    public void run() {
      JsonNode json = mapper.valueToTree(generate(symbol));
      subscription.publish(new PublicationEvent(symbol, "tick", false, json));
    }

    private double roundedRandom(){
      return new BigDecimal(rdm.nextDouble()).setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private Tick generate(String symbol){
      return new Tick(symbol,roundedRandom() , roundedRandom(), roundedRandom());
    }
  }
}
