(ns zmq-stuff.handler
  (:require [compojure.core :refer [GET POST PUT DELETE routes]]))

(def db (atom {"1" {:id 1 :name "August Lilleaas" :profession "Programmer" :email "august@augustl.com"}}))

(defn list-people [db]
  {:status 200
   :body (->> db vals (sort-by :id))})

(defn get-person [db person-id]
  (if-let [person (get db person-id)]
    {:status "ok" :body person}))

(def ring-handler
  (routes
   (GET "/people" [req] (list-people @db))
   (GET "/people/:person-id" [req] (get-person @db (-> req :route-params :person-id)))))


(defn handler [zmq-req]
  (ring-handler {:method (:method zmq-req)
                 :uri (:path zmq-req)
                 :headers (:headers zmq-req)
                 :body (:body zmq-req)}))
