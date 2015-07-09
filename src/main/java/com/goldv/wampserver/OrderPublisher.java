package com.goldv.wampserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.goldv.wampserver.server.PublicationEvent;
import org.goldv.wampserver.server.WAMPPublisher;
import org.goldv.wampserver.server.WAMPSubscriber;
import org.goldv.wampserver.server.WAMPSubscription;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by goldv on 7/8/2015.
 */
public class OrderPublisher implements WAMPPublisher{

  private final String topic;
  private final ScheduledExecutorService scheduler;
  private volatile boolean initialised = false;

  public OrderPublisher(ScheduledExecutorService scheduler, String topic) {
    this.scheduler = scheduler;
    this.topic = topic;
  }

  @Override
  public String topic() {
    return topic;
  }

  @Override
  public void onSubscribe(WAMPSubscriber sub) {
    WAMPSubscription subscription = sub.subscribed();
    if(!initialised) {
      initialised = true;
      scheduler.scheduleAtFixedRate(new Publisher(subscription), 0, 5000, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public void onUnsubscribe(String topic) {
    // TODO
  }

  public static class Publisher implements Runnable{

    private final Map<Order.Status, Order.Status> nextStatus = new HashMap<>();

    private final Random rdm = new Random();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private final WAMPSubscription subscription;

    public Publisher(WAMPSubscription subscription) {
      this.subscription = subscription;

      nextStatus.put(Order.Status.OPEN, Order.Status.PENDING);
      nextStatus.put(Order.Status.PENDING, Order.Status.ACTIVE);
      nextStatus.put(Order.Status.ACTIVE, Order.Status.CLOSED);
    }

    @Override
    public void run() {
      try {
        if (orders.size() < 10) {
          Order order = generateNewOrder();
          orders.put(order.symbol, order);
          publish(order, false);
        }

        if(orders.size() > 3){
          List<String> ids = new ArrayList<>(orders.keySet());
          int updateIdx = Math.abs( rdm.nextInt() ) % (orders.size() - 1);

          String id = ids.get(updateIdx);
          Order currentOrder = orders.get(id);

          Order updatedOrder = currentOrder.withStatus(nextStatus.get(currentOrder.status));
          if (updatedOrder.status.isFinal) {
            orders.remove(updatedOrder.symbol);
          } else {
            orders.put(updatedOrder.symbol, updatedOrder);
          }

          publish(updatedOrder, updatedOrder.status.isFinal);
        }

      } catch(Exception e){
        e.printStackTrace();
      }
    }

    private void publish(Order order, boolean isFinal){
      JsonNode json = mapper.valueToTree(order);
      subscription.publish(new PublicationEvent(String.valueOf(order.symbol), "order", isFinal, json));
    }

    private Order generateNewOrder(){
      int size = 100000 + (int)( 300000 * rdm.nextDouble() );
      return new Order( String.valueOf(Math.abs(rdm.nextInt()) ), size, Order.OrderType.FOK, Order.Status.OPEN );
    }
  }

  public static class Order{

    public Order(String symbol, int size, OrderType orderType, Status status) {
      this.symbol = symbol;
      this.orderType = orderType;
      this.size = size;
      this.status = status;
    }

    public Order withStatus(Status status){
      return new Order(this.symbol, this.size, this.orderType, status);
    }

    enum OrderType{ FOK, MARKET, SL }

    enum Status { OPEN(false), PENDING(false), ACTIVE(false), CLOSED(true);

      public final boolean isFinal;

      Status(boolean isFinal){
        this.isFinal = isFinal;
      }
    }

    public final String symbol;
    public final int size;
    public final OrderType orderType;
    public final Status status;
  }
}
