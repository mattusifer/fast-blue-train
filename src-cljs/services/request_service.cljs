(ns ^{:doc "AngularJS Service to Organize, Make, and Handle requests"}
  fast-blue-train.services.request-service
  (:require [dommy.core :as dommy :refer-macros [sel1]]
            [cljs.core.async :refer [<! timeout]])
  (:use-macros [purnam.core :only [! obj ?]]
               [gyr.core :only [def.factory]]
               [cljs.core.async.macros :only [go-loop]]))

(def.factory fbm.app.RequestService [$q 
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
                    js/google.maps.TravelMode.DRIVING 
                    (not (nil? (? UserService.preferences.carLocation))) 
                    js/google.maps.TravelMode.BICYCLING 
                    (not (nil? (? UserService.preferences.bikeLocation)))}]
         (keys (into {} (filter second modes)))))
     :sendingRequestCallbacks (clj->js [])
     :registerAsSendingRequestObserver 
     (fn [callback-fn]
       (.push (? reqObj.sendingRequestCallbacks) callback-fn))
     :callbackSendingRequest
     (fn []
       (doseq [callback (? reqObj.sendingRequestCallbacks)]
         (callback)))
     :completedRequestCallbacks (clj->js [])
     :registerAsCompletedRequestObserver 
     (fn [callback-fn]
       (.push (? reqObj.completedRequestCallbacks) callback-fn))
     :callbackRequestCompleted 
     (fn [payload]
       (doseq [callback (? reqObj.completedRequestCallbacks)]
         (callback payload)))
     :organizeResponses
     (fn [responses]
       (filter #(not (some nil? (for [x (second %) y (second x)] y)))
               (loop [response (first responses)
                      rem (rest responses)
                      organized {:base {:walking [nil], :transit [nil]}, 
                                 :car {:walking [nil nil], :transit [nil nil]}, 
                                 :bike {:walking [nil nil], :transit [nil nil]}, 
                                 :car-bike {:walking [nil nil nil], 
                                            :transit [nil nil nil]}, 
                                 :bike-car {:walking [nil nil nil], 
                                            :transit [nil nil nil]}}]
                 (if (nil? response) organized
                     (let [prepend (fn [vec el] (into [] (cons el (rest vec))))
                           insert-middle (fn [vec el] 
                                           (if (= (count vec) 3) 
                                             (conj [] (first vec) el (last vec))
                                             (prepend vec el)))
                           append (fn [vec el] (conj (into [] (butlast vec)) el))
                           start (? UserService.preferences.startLocation)
                           end (? UserService.preferences.endLocation)
                           bike (? UserService.preferences.bikeLocation)
                           car (? UserService.preferences.carLocation)
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
                             ;; transit
                             (recur (first rem) (rest rem)
                                    (update-in organized [:base :transit]
                                               #(prepend % response))))
                           bike
                           (if (= mode "WALKING")
                             (recur (first rem) (rest rem)
                                    (-> organized 
                                        (update-in [:bike :walking] 
                                                   #(prepend % response))
                                        (update-in [:bike-car :walking] 
                                                   #(prepend % response))))
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:bike :transit]
                                                   #(prepend % response))
                                        (update-in [:bike-car :transit]
                                                   #(prepend % response)))))
                           car
                           (if (= mode "WALKING")
                             (recur (first rem) (rest rem)
                                    (-> organized 
                                        (update-in [:car :walking] 
                                                   #(prepend % response))
                                        (update-in [:car-bike :walking] 
                                                   #(prepend % response))))
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:car :transit]
                                                   #(prepend % response))
                                        (update-in [:car-bike :transit]
                                                   #(prepend % response))))))
                         (if (= origin bike)
                           (condp = destination
                             end
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:bike :walking]
                                                   #(append % response))
                                        (update-in [:bike :transit]
                                                   #(append % response))
                                        (update-in [:car-bike :walking]
                                                   #(append % response))
                                        (update-in [:car-bike :transit]
                                                   #(append % response))))
                             car
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:bike-car :walking]
                                                   #(insert-middle % response))
                                        (update-in [:bike-car :transit]
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
                                        (update-in [:bike-car :walking]
                                                   #(append % response))
                                        (update-in [:bike-car :transit]
                                                   #(append % response))))
                             bike
                             (recur (first rem) (rest rem)
                                    (-> organized
                                        (update-in [:car-bike :walking]
                                                   #(insert-middle % response))
                                        (update-in [:car-bike :transit]
                                                   #(insert-middle % response))))))))))))
     :handler      
     (fn [responses]
       (let [organized-responses ((? reqObj.organizeResponses) responses)]
         ((? reqObj.callbackRequestCompleted) organized-responses)
         ((? GoogleMapsService.displayRoutes) 
          ((? CostService.getOptimalRoute) 
           (for [x organized-responses y (second x)]
             (second y))))))
     :makeRequests
     (fn [routes delay]

       ;; google maps
       (go-loop [[start end mode :as route] (first routes)
                 remaining (rest routes)
                 promises []]
         (if (nil? route)
           (.then (.all $q (clj->js promises)) (? reqObj.handler) 
                  (fn [err] (.log js/console err) 
                    (.log js/console (str "retrying with delay ") (+ delay 500))
                    ((? reqObj.makeRequests) routes (+ delay 500))))
           (do (<! (timeout delay))
               (let [new-promise-arr (if (= mode "DRIVING") 
                                       '(conj promises 
                                              ((? GoogleMapsService.getDirections) 
                                               (start :address) (end :address) mode))
                                       '(conj promises 
                                              ((? GoogleMapsService.getDirections) 
                                               (start :address) (end :address) mode) 
                                              ((? UberService.getTimeEstimate) (start :lat-long))
                                              ((? UberService.getCostEstimate) 
                                               (start :lat-long) (end :lat-long))))]
                 (recur (first remaining) (rest remaining) (new-promise-arr)))))))
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
                        
                        ;; can't drive to go get the car
                        (and (= (second route) car) 
                             (= mode 
                                js/google.maps.TravelMode.DRIVING))
                        
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
                        
                        ;; we must walk or transit from starting location
                        (and (= (first route) start)
                             (and (not= mode
                                        js/google.maps.TravelMode.WALKING)
                                  (not= mode
                                        js/google.maps.TravelMode.TRANSIT))))
                  nil (conj route mode))))))
     :start
     (fn []
       (let [routes ((? reqObj.gatherRoutes) 
                     (? UserService.preferences.startLocation)
                     (? UserService.preferences.carLocation)
                     (? UserService.preferences.bikeLocation)
                     (? UserService.preferences.endLocation))
             delay (if (< (count routes) 10) 0 600)]
         (when (> delay 0) ((? reqObj.callbackSendingRequest)))
         ((? reqObj.makeRequests) routes delay)))))
  reqObj)
