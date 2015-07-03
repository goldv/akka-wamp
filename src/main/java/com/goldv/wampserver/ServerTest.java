package com.goldv.wampserver;

import com.typesafe.config.ConfigFactory;
import org.goldv.wampserver.server.PublisherContainer;
import org.goldv.wampserver.server.WAMPServer;

import java.util.LinkedList;

/**
 * Created by goldv on 7/3/2015.
 */
public class ServerTest {

  public static void main(String... args){

    new WAMPServer("localhost", 9090, "ws-greeter", null, null, ConfigFactory.defaultReference());
  }
}
