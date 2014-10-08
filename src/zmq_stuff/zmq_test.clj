(ns zmq-stuff.zmq-test
  (:import [org.zeromq ZMQ]
           [java.util.concurrent Executors]))

(defn -main [& args]
  (def out-executor (Executors/newSingleThreadExecutor))

  (def server-ctx (ZMQ/context 1))
  (def dealer (.socket server-ctx ZMQ/DEALER))
  (.bind dealer "tcp://*:1337")
  ;; (.bind dealer "inproc://dealer")
  (dotimes [n 5]
    ((fn [n]
       (doto (Thread.
              (fn []
                (let [sock (doto (.socket server-ctx ZMQ/REP)
                             (.connect "tcp://localhost:1337"))]
                  (while true
                    (.execute out-executor #(prn "Waiting for req"))
                    (let [req (.recv sock 0)]
                      (.execute out-executor #(prn "Got req"))
                      (.send sock (str "pong from " n) ZMQ/NOBLOCK))))))
         (.setName (str "worker-" n))
         (.start)))
     n))

  (Thread/sleep 1000)
  (def client-ctx (ZMQ/context 1))
  (def router (.socket client-ctx ZMQ/ROUTER))
  (.connect router "tcp://localhost:1337")
  (.bind router "inproc://router")

  (dotimes [n 3]
    ((fn [n]
       (doto (Thread.
              (fn []
                (let [sock (doto (.socket client-ctx ZMQ/REQ)
                             (.connect "inproc://router"))]
                  (dotimes [nn 10]
                    (.execute out-executor #(prn "Sending request" sock))
                    (.send sock "ping" 0)
                    (.execute out-executor #(prn "SENT REQUEST"))
                    (let [rep (.recv sock 0)]
                      (.execute out-executor #(prn "GOT REP"))
                      (.execute out-executor #(prn rep)))))))
         (.setName (str "client-" n))
         (.start)))
     n)))
