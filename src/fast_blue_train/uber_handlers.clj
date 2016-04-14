(ns fast-blue-train.uber-handlers
  (:require [clj-http.client :as client]))

(def uber-token "lLwZN5ojjEB3ISysdObEawgUj6zaLtGhqvM7qKGJ")

(defn uber-price-handler 
  [{{start :start end :end} :params :as input}]
  (let [start-lat (first (clojure.string/split start #","))
        start-long (second (clojure.string/split start #","))
        end-lat (first (clojure.string/split end #","))
        end-long (second (clojure.string/split end #","))]
    (client/get "https://api.uber.com/v1/estimates/price" 
                {:accept :json
                 :query-params {:start_latitude start-lat
                                :start_longitude start-long
                                :end_latitude end-lat
                                :end_longitude end-long}
                 :headers {:Authorization (str "Token " uber-token)}
                 :throw-exceptions false})))

(defn uber-time-handler 
  [{{start :start} :params :as input}]
  (let [start-lat (first (clojure.string/split start #","))
        start-long (second (clojure.string/split start #","))]
    (client/get "https://api.uber.com/v1/estimates/time" 
                {:accept :json
                 :query-params {:start_latitude start-lat
                                :start_longitude start-long}
                 :headers {:Authorization (str "Token " uber-token)}
                 :throw-exceptions false})))
