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
     :getOptimalRoute
     (fn [routes]
       ; optimal route based on cost
       (apply min-key
              #(reduce + (map (? costObj.getMonetaryCost) %)) routes)

       ; optimal route based on duration
       ;; (apply min-key
              ;; #(reduce + (map (? GoogleMapsService.getDurationFromResponse)
                            ;; %)) routes)

)))
  costObj)
