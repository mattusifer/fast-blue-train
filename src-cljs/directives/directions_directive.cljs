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
                    (? UserService.preferences.hasCar) 
                    js/google.maps.TravelMode.BICYCLING 
                    (? UserService.preferences.hasBike)}]
         (keys (into {} (filter second modes)))))

     (defn reset-focus []
       (.focus (sel1 :#startLocationInput)))

     (defn get-optimal-route [routes]
       (apply min-key (? GoogleMapsService.getDurationFromResponse) routes))

     (defn handler [responses]
       (.log js/console "responses")
       (.log js/console responses)
       ((? GoogleMapsService.displayRoutes) ;(get-optimal-route responses)
        responses))

     (defn make-requests [routes delay]
       (.log js/console "routes:")
       (.log js/console (clj->js routes))
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
       (filter #(not (nil? %)) 
               (for [route [[start end]
                            (if car [start car] nil)
                            (if car [car end] nil)
                            (if bike [start bike] nil)
                            (if bike [bike end] nil)]
                     mode (available-modes)]
                 (if (or (nil? route) (nil? mode)
                         (and (not= (second route) end)
                              (or (= mode 
                                     js/google.maps.TravelMode.DRIVING)
                                  (= mode 
                                     js/google.maps.TravelMode.BICYCLING))))
                   nil (conj route mode)))))

     (! vm.main (fn []
                  (let [routes (gather-routes (? vm.user.startLocation)
                                              (? vm.user.carLocation)
                                              (? vm.user.bikeLocation)
                                              (? vm.user.endLocation))]
                    (make-requests routes 100))))
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
