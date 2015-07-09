# akka-wamp

This is a WAMP broker ( subscription part and not RPC ) base on AKKA streams/http

To run:

```
mvn compile exec:java
```

This will run the com.goldv.wampserver.ServerTest which provides pubblishers for the topics algotrader.marketdata and algotrader.order.strategy1.

You can then navigate to:

[localhost:9090](localhost:9090)
