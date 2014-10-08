(ns zmq-stuff.exercises.exercise-5-server
  (:require [cheshire.core :refer [parse-string generate-string]]
            [zmq-stuff.exercises.exercise-5-handler :as handler])
  (:import [org.zeromq ZMQ ZMQQueue]
           [java.util.concurrent Executors]))

(defn get-response [req]
  (try
    (or
     (handler/handle (parse-string (String. req "UTF-8")))
     {:status "not-found"})
    (catch Exception e
      (.printStackTrace e)
      {:status "error" :body (.getMessage e)})))

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
                     (.send sock (-> req get-response generate-string (.getBytes "UTF-8")) ZMQ/NOBLOCK))))))
        (.setName (str "worker-" n))
        (.start)))
    (doto (Thread.
           (fn []
             (.run (ZMQQueue. server-ctx router dealer))))
      (.setName "router/dealer pair")
      (.start))))

(defn test-client [connect-to num-reqs]
  (let [client-ctx (ZMQ/context 1)
        reqs [{:method "GET" :path "/people"} {:method "GET" :path "/people/1"} {:method "GET" :path "/people/2"} {:method "GET" :path "/wat"}]
        sock (.socket client-ctx ZMQ/REQ)]
    (.connect sock connect-to)
    (dotimes [n num-reqs]
      (.send sock (-> reqs rand-nth generate-string (.getBytes "UTF-8")) 0)
      (let [rep (.recv sock 0)]
        (println (String. rep "UTF-8"))))
    (.close sock)
    (.term client-ctx)))

(defn -main [& args]
  (create-router-dealer-rep-sockets "tcp://*:5004" 5)

  (Thread/sleep 1000)

  (test-client "tcp://localhost:5004" 10))
