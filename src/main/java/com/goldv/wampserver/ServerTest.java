package com.goldv.wampserver;

import org.goldv.wampserver.server.WAMPServer;
import scala.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by goldv on 7/3/2015.
 */
public class ServerTest {

  public static void main(String... args){

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    List<String> symbols = new ArrayList<>();
    symbols.add("EURUSD");
    symbols.add("EURCHF");
    symbols.add("USDGBP");

    WAMPServer server = new WAMPServer("localhost", 9090,"ws-greeter", "index.html")
      .register( new TickPublisher("algotrader.marketdata", scheduler, symbols) )
      .withDirResource("js", "js")
      .withDirResource("css", "css");

    server.bind();

    System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
    Console.readLine();

  }
}
