package com.goldv.wampserver;

import org.goldv.wampserver.server.WAMPPublisher;
import org.goldv.wampserver.server.WAMPSubscriber;

/**
 * Created by goldv on 7/8/2015.
 */
public class OrderPublisher implements WAMPPublisher {

  //private final String topic;

  @Override
  public String topic() {
    return null;
  }

  @Override
  public void onSubscribe(WAMPSubscriber sub) {

  }

  @Override
  public void onUnsubscribe(String topic) {

  }
}
