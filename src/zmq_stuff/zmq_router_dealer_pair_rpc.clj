(ns zmq-stuff.zmq-router-dealer-pair-rpc
  (:import [org.zeromq ZMQ ZMQQueue]
           [java.util.concurrent Executors]))

(defn -main [& args]
  (def server-ctx (ZMQ/context 1))
  (def server-router (.socket server-ctx ZMQ/ROUTER))
  (def server-dealer (.socket server-ctx ZMQ/DEALER))

  (.bind server-router "tcp://*:1337")
  (.bind server-dealer "inproc://rep-worker")
  (dotimes [n 5]
    (doto (Thread. (fn []
                     (let [sock (doto (.socket server-ctx ZMQ/REP)
                                  (.connect "inproc://rep-worker"))]
                       (while true
                         (let [req (.recv sock 0)]
                           (.send sock (str n ": " (String. req)) ZMQ/NOBLOCK))))))
      (.setName (str "worker-" n))
      (.start)))
  (doto (Thread. (fn []
                   (.run (ZMQQueue. server-ctx server-router server-dealer))))
    (.setName "router/dealer pair")
    (.start))




  (Thread/sleep 1000)




  (def client-ctx (ZMQ/context 1))
  (def client-dealer (.socket client-ctx ZMQ/DEALER))
  (def client-router (.socket client-ctx ZMQ/ROUTER))

  (.connect client-dealer "tcp://localhost:1337")
  (.bind client-router "inproc://req-worker")

  (def out-executor (Executors/newSingleThreadExecutor))

  (dotimes [n 5]
    (doto (Thread.
           (fn []
             (let [sock (doto (.socket client-ctx ZMQ/REQ)
                          (.connect "inproc://req-worker"))]
               (dotimes [nn 100]
                 (.send sock (str "hi from " n "/" nn) 0)
                 (let [rep (.recv sock 0)]
                   (.execute out-executor #(println (String. rep))))))))
      (.setName (str "client-" n))
      (.start)))

  (doto (Thread.
         (fn []
           (.run (ZMQQueue. client-ctx client-dealer client-router))))
    (.setName "dealer/router pair")
    (.start)))
