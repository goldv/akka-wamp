feed = (function () {

    var topicsCallbacks = {}
    var session

    var connection = new autobahn.Connection({
        url: 'ws://localhost:9090/ws-greeter',
        realm: 'realm1'
    });

    connection.onopen = function (s) {
        console.log("session open")

        session = s
        for(topic in topicsCallbacks){
            var callback = topicsCallbacks[topic]
            subscribe(topic, callback)
        }
    }

    connection.open()

    function subscribe(topic, callback){
        session.subscribe(topic, function(event){
            callback(event)
            console.log("event received " + event + " topic = " + topic)
        });
    }

    return {
        watch: function(topics, callback) {
            for(idx in topics){
                topicsCallbacks[topics[idx]] = callback
                if(session){
                    subscribe(topic, callback)
                }
            }
        },
        unwatch: function(symbol) {
            // TODO
        }
    };

}());