(ns zmq-stuff.rep-server
  (:require [cheshire.core :refer [generate-string parse-string]]
            zmq-stuff.handler)
  (:import [org.zeromq ZMQ ZMQQueue]))

(def default-bind-to "tcp://127.0.0.1:1337")
(def charset "UTF-8")

(defn get-response [req]
  (try
    (or
     (zmq-stuff.handler/handler (parse-string (String. req charset)))
     {:status "not-found"})
    (catch Exception e
      {:status "error" :body (.getMessage e)} charset)))

(defn create-rep-handler [ctx connect-to]
  (let [sock (doto (.socket ctx ZMQ/REP)
               (.connect connect-to))]
    (while true
      (let [req (.recv sock 0)]
        (.send sock (-> req get-response generate-string (.getBytes charset)) ZMQ/NOBLOCK)))))

(defn create-router-dealer [bind-to num-threads]
  ;; (let [ctx (ZMQ/context 1)
  ;;       worker-url "inproc://rpc-dealers"
  ;;       queue (ZMQQueue. ctx
  ;;                        (doto (.socket ctx ZMQ/ROUTER)
  ;;                          (.bind bind-to))
  ;;                        (doto (.socket ctx ZMQ/DEALER)
  ;;                          (.bind worker-url)))]
  ;;   (doto (Thread. #(.run queue)))
  ;;   (doall (for [n (range num-threads)]
  ;;            (doto (Thread. (create-rep-handler ctx worker-url))
  ;;              (.setName (str "handler-" n))
  ;;              (.start)))))
  )

(defn create-executor [])
