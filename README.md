# akka-wamp

This is a [WAMP](http://wamp.ws/) broker ( subscription part and not RPC ) base on AKKA streams/http. For the front end I'm using the [autobahn](http://autobahn.ws/js/) implementation of WAMP.

To run:

```
mvn compile exec:java
```
You can then navigate to:

[localhost:9090](http://localhost:9090)

This will run the com.goldv.wampserver.ServerTest which provides pubblishers for the topics algotrader.marketdata and algotrader.order.strategy1.

Published events are cached for both topics so that new subscribers can get the most recent view. For the algotrader.order.strategy1 topic, we reach a terminal state when the order reaches the CLOSED state. This means it is removed from the cache in addition to being removed from the table in the front end.


