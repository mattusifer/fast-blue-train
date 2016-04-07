(ns ^{:doc "AngularJS Directive for Getting Directions"}
  fast-blue-train.directives.directions-directive
  (:require [fast-blue-train.services.google-maps-service :as maps]
            [dommy.core :as dommy :refer-macros [sel1 sel]]
            [cljs.core.async :refer [<! timeout]])
  (:use-macros [cljs.core.async.macros :only [go-loop]]
               [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.directions [$q 
                                   UserService 
                                   GoogleMapsService]
  (obj
   :restrict "E"
   :templateUrl "angular/src/partials/directions.html"
   :scope {}
   :controllerAs "vm"
   :bindToController true
   :controller 
   (fn [] 
     (def vm this)
     (! vm.initview true)
     (! vm.start "")
     (! vm.end "")
     (! vm.switchView (fn [] (! vm.initview false)))
     (! vm.user (? UserService.preferences))
     (! vm.setPreference
        (fn [pref val] ((? UserService.setPreference) pref val)))

     (defn available-modes []
       (let [modes {js/google.maps.TravelMode.WALKING true
                    js/google.maps.TravelMode.TRANSIT true
                    js/google.maps.TravelMode.DRIVING 
                    (not (nil? (? UserService.preferences.carLocation))) 
                    js/google.maps.TravelMode.BICYCLING 
                    (not (nil? (? UserService.preferences.bikeLocation)))}]
         (keys (into {} (filter second modes)))))

     (defn reset-focus []
       (.focus (sel1 :#startLocationInput)))

     (defn get-optimal-route 
       "return the best route"
       [routes]
       (.log js/console "routes")
       (.log js/console (clj->js routes))

       (apply min-key
              #(reduce + 
                       (map (? GoogleMapsService.getDurationFromResponse)
                            %)) routes))

     (defn organize-responses 
       "organize raw responses into complete routes"
       [responses]
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
                   start (str (? vm.user.startLocation))
                   end (? vm.user.endLocation)
                   bike (? vm.user.bikeLocation)
                   car (? vm.user.carLocation)
                   origin (? response.request.origin)
                   destination (str (? response.request.destination))
                   mode (? response.request.travelMode)]
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
                                           #(insert-middle % response)))))))))))

     (defn handler [responses]
       (let [organized-responses (organize-responses responses)]
         ((? GoogleMapsService.displayRoutes) 
          (get-optimal-route (for [x organized-responses y (second x)]
                               (second y))))))

     (defn make-requests [routes delay]
       (go-loop [[start end mode :as route] (first routes)
                 remaining (rest routes)
                 promises []]
         (if (nil? route)
           (.then (.all $q (clj->js promises)) handler 
                  (fn [err] (.log js/console err) 
                    (.log js/console (str "retrying with delay ") (+ delay 500))
                    (make-requests routes (+ delay 500))))
           (do (<! (timeout delay))
               (recur (first remaining) (rest remaining) 
                      (conj promises ((? GoogleMapsService.getDirections) start end mode)))))))

     (defn gather-routes
       "return all available routes"
       [start car bike end]
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
                    mode (available-modes)]
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

     (! vm.main (fn []
                  (let [delay 800 ;; delay between requests
                        routes (gather-routes (? vm.user.startLocation)
                                              (? vm.user.carLocation)
                                              (? vm.user.bikeLocation)
                                              (? vm.user.endLocation))]
                    (make-requests routes delay))))
     vm)

   :link
   (fn [scope elm attr controller]
     (let [start-elem (sel1 :#startLocationInput)
           end-elem (sel1 :#endLocationInput)
           submit-elem (sel1 :#direction-submit)
           ac-opts (? GoogleMapsService.autocompleteOpts)
           start-ac 
           (js/google.maps.places.Autocomplete. start-elem ac-opts)
           end-ac
           (js/google.maps.places.Autocomplete. end-elem ac-opts)]

       (.addListener js/google.maps.event start-ac "place_changed"
                     (fn [] (let [place (.getPlace this)]
                              ((? controller.setPreference) 
                               "startLocation"
                               (? place.formatted_address)))))

       (.addListener js/google.maps.event end-ac "place_changed"
                     (fn [] (let [place (.getPlace this)]
                              ((? controller.setPreference) 
                               "endLocation"
                               (? place.formatted_address)))))

       ;Submit 
       (dommy/listen! submit-elem "click" (? controller.main))))))
