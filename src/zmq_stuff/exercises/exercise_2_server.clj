(ns zmq-stuff.exercises.exercise-2-server
  (:import [org.zeromq ZMQ ZMQQueue]))

(defn create-rep-socket [bind-to id]
  (let [server-ctx (ZMQ/context 1)
        sock (.socket server-ctx ZMQ/REP)]
    (.bind sock bind-to)
    (while true
      (let [req (.recv sock 0)]
        (.send sock (str "Pong " id ": " (String. req "UTF-8")) ZMQ/NOBLOCK)))))

(defn test-client [host start-port]
  (let [client-ctx (ZMQ/context 1)
        req-sock (.socket client-ctx ZMQ/REQ)]
    (dotimes [n 3]
      (.connect req-sock (str "tcp://" host ":" (+ start-port n))))
    (dotimes [n 20]
      (.send req-sock (str "hi from req " n))
      (let [res (.recv req-sock 0)]
        (println "Got res:" (String. res "UTF-8"))))
    (.close req-sock)
    (.term client-ctx)))

(defn -main [& args]
  (dotimes [n 3]
    (doto (Thread. (fn []  (create-rep-socket (str "tcp://*:" (+ 5001 n)) n)))
      (.setName (str "ZMQ rep " n))
      (.start)))

  (test-client "localhost" 5001))
