package com.goldv.wampserver;

import org.goldv.wampserver.server.WAMPServer;
import scala.Console;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by goldv on 7/3/2015.
 */
public class ServerTest {

  public static void main(String... args){

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    WAMPServer server = new WAMPServer("localhost", 9090,"ws-greeter", "index.html")
      .register( new TickPublisher("algotrader.marketdata.EURUSD", scheduler) )
      .register(new TickPublisher("algotrader.marketdata.EURCHF", scheduler))
      .register( new TickPublisher("algotrader.marketdata.USDGBP", scheduler) )
      .withDirResource("js", "js");

    server.bind();

    System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
    Console.readLine();

  }
}
