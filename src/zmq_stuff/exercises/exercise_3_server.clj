(ns zmq-stuff.exercises.exercise-3-server
  (:import [org.zeromq ZMQ ZMQQueue]
           [java.util.concurrent Executors]))

(defn create-router-dealer-rep-sockets [bind-to num-rep-sockets]
  (let [server-ctx (ZMQ/context 1)
        router (.socket server-ctx ZMQ/ROUTER)
        dealer (.socket server-ctx ZMQ/DEALER)]
    (.bind router bind-to)
    (.bind dealer "inproc://rep-worker")

    (dotimes [n num-rep-sockets]
      (doto (Thread.
             (fn []
               (let [sock (.socket server-ctx ZMQ/REP)]
                 (.connect sock "inproc://rep-worker")
                 (while true
                   (let [req (.recv sock 0)]
                     (.send sock (str n ": " (String. req "UTF-8")) ZMQ/NOBLOCK))))))
        (.setName (str "worker-" n))
        (.start)))
    (doto (Thread.
           (fn []
             (.run (ZMQQueue. server-ctx router dealer))))
      (.setName "router/dealer pair")
      (.start))))

(defn test-client [connect-to num-threads num-reqs]
  (let [client-ctx (ZMQ/context 1)
        out-executor (Executors/newSingleThreadExecutor)]
    (dotimes [n num-threads]
      (doto (Thread.
             (fn []
               (let [sock (.socket client-ctx ZMQ/REQ)]
                 (.connect sock connect-to)
                 (dotimes [nn num-reqs]
                   (.send sock (str "hi from " n "/" nn) 0)
                   (let [rep (.recv sock 0)]
                     (.execute out-executor #(println (String. rep)))))
                 (.close sock))))
        (.setName (str "client-" n))
        (.start)))))

(defn -main [& args]
  (create-router-dealer-rep-sockets "tcp://*:5004" 5)

  (Thread/sleep 1000)

  (test-client "tcp://localhost:5004" 5 50))
