(ns ^{:doc "AngularJS Service to Calculate Cost of a Route"}
  fast-blue-train.services.cost-service
  (:require [dommy.core :as dommy :refer-macros [sel1]])
  (:use-macros [purnam.core :only [! obj ?]]
               [gyr.core :only [def.factory]]))

(def.factory fbm.app.CostService [GoogleMapsService UserService]
  (def costObj
    (obj 
     :costConstants (obj 
                     :gas 2.194
                     :transit 1.80)
     :getMonetaryCost
     (fn [response]
       (let [mode ((? GoogleMapsService.getTransportModeFromResponse)
                   response)
             distance ((? GoogleMapsService.getMilesFromResponse)
                       response)
             mpg (or (? UserService.preferences.carMPG) 20)]
         (case mode
           "TRANSIT" (? costObj.costConstants.transit)
           "DRIVING" (* (/ (? costObj.costConstants.gas) mpg) distance)
           0)))
     :organizeRoutes
     (fn [routes]
       (let [agg-duration 
             (fn [route-coll]
               (reduce + (map (? GoogleMapsService.getDurationFromResponse) 
                              route-coll)))
             agg-monetary-cost
             (fn [route-coll]
               (reduce + (map (? costObj.getMonetaryCost) route-coll)))]
         (loop [possibilities (conj '() (first routes))
                rem (rest routes)]
           (if (empty? rem)
             (into (sorted-set-by 
                    (fn [e1 e2] 
                      (let [comparison 
                            (compare (agg-duration e1)
                                     (agg-duration e2))]
                        (if (not= comparison 0) 
                          comparison
                          1)))) 
                   possibilities)
             (if (and (<= (agg-monetary-cost (first rem))
                          (? UserService.preferences.budget)))
               (recur (conj possibilities (first rem)) (rest rem))
               (recur possibilities (rest rem)))))))
     :getOptimalRoute
     (fn [routes]
       (let [organized ((? costObj.organizeRoutes) routes)]
         ;; (.log js/console (clj->js organized))
         (first organized)))))
  costObj)
