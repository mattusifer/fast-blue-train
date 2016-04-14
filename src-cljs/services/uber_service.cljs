(ns ^{:doc "AngularJS Service to interface with the Uber API"}
  fast-blue-train.services.uber-service
  (:require [dommy.core :as dommy :refer-macros [sel1]]
            [ajax.core :refer [GET]]
            [cljs.core.async :refer [<! timeout]])
  (:use-macros [purnam.core :only [! obj ?]]
               [gyr.core :only [def.factory]]
               [cljs.core.async.macros :only [go]]))

(def.factory fbm.app.UberService [$q]
  (def uberObj
    (obj 
     :getPriceEstimate 
     (fn [start end]
       (let [deferred (.defer $q)
             handler 
             (fn [resp]
               (.resolve deferred resp))
             err-handler 
             (fn [resp]
               (.reject deferred (str "Failed due to " resp)))]
         (GET "/uber-price" 
              {:params {:start start
                        :end end}
               :handler handler
               :error-handler err-handler
               :response-format :json})
         (.then (.-promise deferred)
                (fn [resp] (clj->js (assoc resp 
                                           :type "UBER-PRICE" 
                                           :request {:origin (clojure.string/split start #",")})))
                (fn [err] (.reject $q err)))))
     :getTimeEstimate
     (fn [start]
       (let [deferred (.defer $q)
             handler 
             (fn [resp]
               (.resolve deferred resp))
             err-handler 
             (fn [resp]
               (.reject deferred (str "Failed due to " resp)))]
         (GET "/uber-time"
              {:params {:start start}
               :handler handler
               :error-handler err-handler
               :response-format :json})
         (.then (.-promise deferred)
                (fn [resp] (clj->js (assoc resp 
                                           :type "UBER-TIME"
                                           :request {:origin (clojure.string/split start #",")})))
                (fn [err] (.reject $q err)))))))
  uberObj)
