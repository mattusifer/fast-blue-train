(ns ^{:doc "AngularJS Service to Calculate Cost of a Route"}
  fast-blue-train.services.cost-service
  (:require [dommy.core :as dommy :refer-macros [sel1]])
  (:use-macros [purnam.core :only [! obj ?]]
               [gyr.core :only [def.factory]]))

(def.factory fbm.app.CostService [GoogleMapsService UserService]
  (def costObj
    (obj 
     :costConstants (obj 
                     :gas 2.194 ; avg gas price in philly
                     :transit 1.80
                     :defaultMPG 20)
     :getMonetaryCost
     (fn [response]
       (let [mode ((? GoogleMapsService.getTransportModeFromResponse)
                   (clj->js response))
             distance ((? GoogleMapsService.getMilesFromResponse)
                       (clj->js response))
             mpg (or (? UserService.preferences.carMPG) (? costObj.costConstants.defaultMPG))]
         (condp re-matches mode
           #"TRANSIT" (if (? UserService.preferences.transPass) 0 (? costObj.costConstants.transit))
           #"DRIVING" (* (/ (? costObj.costConstants.gas) mpg) distance)
           #"UBER - .*" (:cost-usd (:uber-stats response))
           0)))
     :getDuration
     (fn [response]
       (if (nil? (:uber-stats response))
         ((? GoogleMapsService.getDurationFromResponse) 
          (clj->js response))
         (+ ((? GoogleMapsService.getDurationFromResponse) 
             (clj->js response))
            (:time-sec (:uber-stats response)))))
     :organizeRoutes
     (fn [routes]
       (let [agg-duration 
             (fn [route-coll]
               (reduce + (map (? costObj.getDuration) route-coll)))
             agg-monetary-cost
             (fn [route-coll]
               (reduce + (map (? costObj.getMonetaryCost) route-coll)))]
         (loop [route (first routes)
                rem (rest routes)
                possibilities '()]
           (if (nil? route)
             (into (sorted-set-by 
                    (fn [e1 e2] 
                      (let [duration-comparison 
                            (compare (agg-duration e1)
                                     (agg-duration e2))
                            monetary-comparison
                            (compare (agg-monetary-cost e1)
                                     (agg-monetary-cost e2))]
                        (if (not= duration-comparison 0) 
                          duration-comparison
                          (if (not= monetary-comparison 0)
                            monetary-comparison 1))))) 
                   possibilities)
             (if (<= (agg-monetary-cost route)
                     (or (? UserService.preferences.budget) 0))
               (recur (first rem) (rest rem)
                      (conj possibilities route))
               (recur (first rem) (rest rem) possibilities))))))
     :getOptimalRoute
     (fn [routes]
       (let [organized ((? costObj.organizeRoutes) routes)]
         (clj->js (first organized))))))
  costObj)
