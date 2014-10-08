(ns zmq-stuff.exercises.exercise-5-handler
  (:require [compojure.core :refer [GET POST PUT DELETE routes]]))

(def db (atom {"1" {:id 1 :name "August Lilleaas" :profession "Programmer" :email "august@augustl.com"}
               "2" {:id 2 :name "Mike Rowe" :profession "Dirty"}}))

(defn list-people [db]
  {:status "ok"
   :body (->> db vals (sort-by :id))})

(defn get-person [db person-id]
  (if-let [person (get db person-id)]
    {:status "ok" :body person}))

(def ring-handler
  (routes
   (GET "/people" req (list-people @db))
   (GET "/people/:person-id" req (get-person @db (-> req :route-params :person-id)))))

(defn handle [zmq-req]
  (ring-handler {:request-method (-> (zmq-req "method") .toLowerCase keyword)
                 :uri (zmq-req "path")
                 :headers (zmq-req "headers")
                 :body (zmq-req "body")}))
