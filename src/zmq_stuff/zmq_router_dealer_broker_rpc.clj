(ns zmq-stuff.zmq-router-dealer-broker-rpc
  (:import [org.zeromq ZMQ ZMQQueue]
           [java.util.concurrent Executors]))

(defn -main [& args]
  (def server-ctx (ZMQ/context 1))
  (def server-router (.socket server-ctx ZMQ/ROUTER))
  (def server-dealer (.socket server-ctx ZMQ/DEALER))

  (.bind server-router "tcp://*:5004")
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

  (def out-executor (Executors/newSingleThreadExecutor))

  (dotimes [n 5]
    (doto (Thread.
           (fn []
             (let [sock (doto (.socket client-ctx ZMQ/REQ)
                          (.connect "tcp://localhost:5004"))]
               (dotimes [nn 100]
                 (.send sock (str "hi from " n "/" nn) 0)
                 (let [rep (.recv sock 0)]
                   (.execute out-executor #(println (String. rep))))))))
      (.setName (str "client-" n))
      (.start))))
