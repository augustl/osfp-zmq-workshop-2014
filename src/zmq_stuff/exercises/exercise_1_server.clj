(ns zmq-stuff.exercises.exercise-1-server
  (:import [org.zeromq ZMQ ZMQQueue]))

(defn create-rep-socket [bind-to]
  (let [server-ctx (ZMQ/context 1)
        sock (.socket server-ctx ZMQ/REP)]
    (.bind sock bind-to)
    (while true
      (let [req (.recv sock 0)]
        (.send sock (str "Pong: " (String. req "UTF-8")) ZMQ/NOBLOCK)))))


(defn test-client [connect-to]
  (let [client-ctx (ZMQ/context 1)
        req-sock (.socket client-ctx ZMQ/REQ)]
    (.connect req-sock connect-to)
    (dotimes [n 5]
      (.send req-sock (str "hi from req " n))
      (let [res (.recv req-sock 0)]
        (println "Got res:" (String. res "UTF-8"))))
    (.close req-sock)
    (.term client-ctx)))

(defn -main [& args]
  (doto (Thread. (fn [] (create-rep-socket "tcp://*:5000")))
    (.setName "ZMQ rep")
    (.start))

  (test-client "tcp://localhost:5000"))
