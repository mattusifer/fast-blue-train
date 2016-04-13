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
     :priceHandler 
     (fn [response] response)
     :timeHandler
     (fn [response] response)
     :getPriceEstimate 
     (fn [start end]
       (let [deferred (.defer $q)
             handler 
             (fn [resp]
               (if (= (resp :status) 200)
                 (.resolve deferred (resp :body))
                 (.reject deferred (str "Failed due to " (resp :body)))))]
         (GET "/uber-price" 
              {:params {:start start
                        :end end}
               :handler (? uberObj.priceHandler)})
         (.then (.-promise deferred)
                (fn [resp] resp)
                (fn [err] (.reject $q err)))))
     :getTimeEstimate
     (fn [start product_id]
       (if (nil? product_id)
         (GET "/uber-time"
              {:params {:start start}
               :handler (? uberObj.timeHandler)})
         (GET "/uber-time"
              {:params {:start start
                        :product_id product_id}
               :handler (? uberObj.timeHandler)})))))
  uberObj)
