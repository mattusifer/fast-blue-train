(ns ^{:doc "AngularJS Service to Organize, Make, and Handle requests"}
  fast-blue-train.services.request-service
  (:require [dommy.core :as dommy :refer-macros [sel1]]
            [cljs.core.async :refer [<! timeout]]
            [clojure.set])
  (:use-macros [purnam.core :only [! obj ?]]
               [gyr.core :only [def.factory]]
               [cljs.core.async.macros :only [go-loop]]))

(def.factory fbm.app.RequestService [$q 
                                     $rootScope
                                     UserService 
                                     GoogleMapsService 
                                     CostService
                                     UberService]
  (def reqObj
    (obj
     :availableModes 
     (fn []
       (let [modes {js/google.maps.TravelMode.WALKING true
                    js/google.maps.TravelMode.TRANSIT true
                    js/google.maps.TravelMode.DRIVING true
                    js/google.maps.TravelMode.BICYCLING 
                    (not (nil? (? UserService.preferences.bikeLocation)))}]
         (keys (into {} (filter second modes)))))
     :broadcastSendingRequest
     (fn [] (.$broadcast $rootScope "sendingRequests"))
     :broadcastRequestCompleted 
     (fn [payload] (.$broadcast $rootScope "requestsComplete" 
                                (obj :organizedResponses
                                     payload)))
     :organizeResponses
     (fn [responses]
       (filter #(not (some nil? (for [x (second %) y (second x)] y)))
               (loop [response (first responses)
                      rem (rest responses)
                      organized {:base {:walking [nil], :transit [nil], :uber [nil]}, 
                                 :car {:walking [nil nil], :transit [nil nil], :uber [nil nil]}, 
                                 :bike {:walking [nil nil], :transit [nil nil], :uber [nil nil]}, 
                                 :car-bike {:walking [nil nil nil], 
                                            :transit [nil nil nil]
                                            :uber [nil nil nil]}, 
                                 :bike-car {:walking [nil nil nil], 
                                            :transit [nil nil nil]
                                            :uber [nil nil nil]}}]
                 (if (nil? response) organized
                     (let [prepend (fn [vec el] (into [] (cons el (rest vec))))
                           insert-middle (fn [vec el] 
                                           (if (= (count vec) 3) 
                                             (conj [] (first vec) el (last vec))
                                             (prepend vec el)))
                           append (fn [vec el] (conj (into [] (butlast vec)) el))
                           start (? UserService.preferences.startLocation.address)
                           end (? UserService.preferences.endLocation.address)
                           bike (? UserService.preferences.bikeLocation.address)
                           car (? UserService.preferences.carLocation.address)
                           origin (? response.request.origin)
                           destination (str (? response.request.destination))
                           mode 
                           ((? GoogleMapsService.getTransportModeFromResponse)
                            response)]

                       (if (= origin start)
                         (condp = destination
                           end 
                           (if (= mode "WALKING") 
                             (recur (first rem) (rest rem) 
                                    (update-in organized [:base :walking] 
                                               #(prepend % response)))
                             (if (= mode "TRANSIT")
                               (recur (first rem) (rest rem)
                                      (update-in organized [:base :transit]
                                                 #(prepend % response)))
                               (recur (first rem) (rest rem)
                                      (update-in organized [:base :uber]
                                                 #(prepend % response)))))
                           bike
                           (if (= mode "WALKING")
                             (recur (first rem) (rest rem)
                                    (-> organized 
                                        (update-in [:bike :walking] 
                                                   #(prepend % response))
                                        (update-in [:bike-car :walking] 
                                                   #(prepend % response))))
                             (if (= mode "TRANSIT") 
                               (recur (first rem) (rest rem)
                                      (-> organized
                                          (update-in [:bike :transit]
                                                     #(prepend % response))
                                          (update-in [:bike-car :transit]
                                                     #(prepend % response))))
                               (recur (first rem) (rest rem)
                                      (-> organized
                                          (update-in [:bike :uber]
                                                     #(prepend % response))
                                          (update-in [:bike-car :uber]
                                                     #(prepend % response))))))
                           car
                           (if (= mode "WALKING")
                             (recur (first rem) (rest rem)
                                    (-> organized 
                                        (update-in [:car :walking] 
                                                   #(prepend % response))
                                        (update-in [:car-bike :walking] 
                                                   #(prepend % response))))
                             (if (= mode "TRANSIT") 
                               (recur (first rem) (rest rem)
                                      (-> organized
                                          (update-in [:car :transit]
                                                     #(prepend % response))
                                          (update-in [:car-bike :transit]
                                                     #(prepend % response))))
                               (recur (first rem) (rest rem)
                                      (-> organized
                                          (update-in [:car :uber]
                                                     #(prepend % response))
                                          (update-in [:car-bike :uber]
                                                     #(prepend % response)))))))
                         (if (= origin bike)
                           (condp = destination
                             end
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:bike :walking]
                                                   #(append % response))
                                        (update-in [:bike :transit]
                                                   #(append % response))
                                        (update-in [:bike :uber]
                                                   #(append % response))
                                        (update-in [:car-bike :walking]
                                                   #(append % response))
                                        (update-in [:car-bike :transit]
                                                   #(append % response))
                                        (update-in [:car-bike :uber]
                                                   #(append % response))))
                             car
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:bike-car :walking]
                                                   #(insert-middle % response))
                                        (update-in [:bike-car :transit]
                                                   #(insert-middle % response))
                                        (update-in [:bike-car :uber]
                                                   #(insert-middle % response)))))
                           ;; car location
                           (condp = destination
                             end
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:car :walking]
                                                   #(append % response))
                                        (update-in [:car :transit]
                                                   #(append % response))
                                        (update-in [:car :uber]
                                                   #(append % response))
                                        (update-in [:bike-car :walking]
                                                   #(append % response))
                                        (update-in [:bike-car :transit]
                                                   #(append % response))
                                        (update-in [:bike-car :uber]
                                                   #(append % response))))
                             bike
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:car-bike :walking]
                                                   #(insert-middle % response))
                                        (update-in [:car-bike :transit]
                                                   #(insert-middle % response))
                                        (update-in [:car-bike :uber]
                                                   #(insert-middle % response))))))))))))
     :addUberToOrganizedResponses
     (fn [organized-google-responses
          uber-responses]
       (loop [entry (js->clj (first organized-google-responses)
                             :keywordize-keys true)
              rem (js->clj (rest organized-google-responses)
                           :keywordize-keys true)
              full-map {}]
         (if (nil? entry) 
           (clj->js full-map)
           (let [uber-designated-route (first (:uber (val entry)))
                 uber-times (:times (first (filter #(= (:type %) "UBER-TIME") 
                                                   uber-responses)))
                 uber-prices (:prices (first (filter #(= (:type %) "UBER-PRICE")
                                                     uber-responses)))
                 finalized-routes 
                 (apply merge 
                        (for [uber-option uber-times]
                          ;; expand designated uber entry (yikes)
                          (assoc {} 
                                 (keyword (:display_name uber-option))
                                 (filter #(not (empty? %)) 
                                         (into [] (cons (assoc uber-designated-route 
                                                               :uber-stats
                                                               {:time-sec (:estimate uber-option)
                                                                :cost-usd (:high_estimate 
                                                                           (first (filter #(= (:display_name %) 
                                                                                              (:display_name uber-option))
                                                                                          uber-prices)))}) (rest (:uber (val entry)))))))))]
             (recur (first rem) (rest rem) 
                    (assoc full-map (key entry) (merge finalized-routes (dissoc (val entry) :uber))))))))
     :handler      
     (fn [responses]
       (let [uber-responses (filter #(or (= (:type %) "UBER-TIME")
                                         (= (:type %) "UBER-PRICE"))
                                    (js->clj responses :keywordize-keys true))
             google-responses (clj->js (filter #(and (not= (:type %) "UBER-TIME")
                                                     (not= (:type %) "UBER-PRICE"))
                                               (js->clj responses :keywordize-keys true)))
             organized-responses (-> google-responses
                                     ((? reqObj.organizeResponses))
                                     ((? reqObj.addUberToOrganizedResponses)
                                      uber-responses))
             routes (for [x (js->clj organized-responses
                                     :keywordize-keys true) y 
                          (second x)]
                      (second y))]
         ((? GoogleMapsService.displayRoutes) 
          ((? CostService.getOptimalRoute) routes))
         ((? reqObj.broadcastRequestCompleted) 
          ((? CostService.organizeRoutes) routes))))
     :makeRequests
     (fn [routes delay]
       (go-loop [[start end mode :as route] (first routes)
                 remaining (rest routes)
                 promises [((? UberService.getTimeEstimate) (? start.lat-long))]]
         (if (nil? route)
           (.then (.all $q (clj->js promises)) (? reqObj.handler) 
                  (fn [err] (.log js/console err) 
                    (.log js/console (str "retrying with delay ") (+ delay 500))
                   ((? reqObj.makeRequests) routes (+ delay 500))))
           (do (<! (timeout delay))
               (recur (first remaining) (rest remaining) 
                      (if (= mode "WALKING") 
                        (conj promises ((? GoogleMapsService.getDirections) 
                                        (? start.address) (? end.address) mode) 
                              ((? UberService.getPriceEstimate) 
                               (? start.lat-long) (? end.lat-long)))
                        (conj promises ((? GoogleMapsService.getDirections) 
                                        (? start.address) (? end.address) mode))))))))
     :gatherRoutes
     (fn [start car bike end]
       (filter 
        #(not (nil? %)) 
        (into #{} 
              (for [route [[start end]
                           (if car [start car] nil)
                           (if car [car end] nil)
                           (if bike [start bike] nil)
                           (if bike [bike end] nil)
                           (if (and car bike) [bike car] nil)
                           (if (and car bike) [car bike] nil)]
                    mode ((? reqObj.availableModes))]
                (if (or (nil? route) (nil? mode)
                        
                        ;; can't bike to go get the bike
                        (and (= (second route) bike) 
                             (= mode 
                                js/google.maps.TravelMode.BICYCLING))
                        
                        ;; if we just picked up the bike, we must bike
                        (and (= (first route) bike)
                             (not= mode
                                   js/google.maps.TravelMode.BICYCLING))
                        
                        ;; if we just picked up the car, we must drive
                        (and (= (first route) car)
                             (not= mode
                                   js/google.maps.TravelMode.DRIVING))
                        
                        ;; we must walk, transit, or uber from starting location
                        (and (= (first route) start)
                             (and (not= mode
                                        js/google.maps.TravelMode.WALKING)
                                  (not= mode
                                        js/google.maps.TravelMode.TRANSIT)
                                  (not= mode
                                        js/google.maps.TravelMode.DRIVING))))
                  nil (conj route mode))))))
     :start
     (fn []
       (let [routes ((? reqObj.gatherRoutes) 
                     (? UserService.preferences.startLocation)
                     (? UserService.preferences.carLocation)
                     (? UserService.preferences.bikeLocation)
                     (? UserService.preferences.endLocation))
             delay (if (< (count routes) 10) 0 600)]
         (when (> delay 0) ((? reqObj.broadcastSendingRequest)))
         ((? reqObj.makeRequests) routes delay)))))
  reqObj)
