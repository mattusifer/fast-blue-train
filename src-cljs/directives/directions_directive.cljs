(ns ^{:doc "AngularJS Directive for Getting Directions"}
  fast-blue-train.directives.directions-directive
  (:require [fast-blue-train.services.google-maps-service :as maps]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj !]]
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

     (def modes [js/google.maps.TravelMode.DRIVING
                 js/google.maps.TravelMode.WALKING
                 js/google.maps.TravelMode.BICYCLING
                 js/google.maps.TravelMode.TRANSIT])

     (defn reset-focus []
       (.focus (sel1 :#startLocationInput)))

     (defn get-duration [response]
       (.-value (.-duration (first (.-legs (first (.-routes response)))))))
 
     (defn get-optimal-route [routes]
       (apply min-key get-duration routes))

     (defn get-route
       "Get shortest route between start and destination"
       [start end]
       (let [promises (map (partial (.-getDirections GoogleMapsService) 
                                    start end) modes)
             handler (fn [values]
                       ((.-displayRoute GoogleMapsService) (get-optimal-route values))
                       (reset-focus))]
         (.then (.all $q (into-array promises)) handler)))
     vm)

   :link
   (fn [scope elm attr]
     (let [start-elem (sel1 :#startLocationInput)
           end-elem (sel1 :#endLocationInput)
           submit-elem (sel1 :#direction-submit)
           ac-opts (.-autocompleteOpts GoogleMapsService)
           start-input (js/google.maps.places.Autocomplete. start-elem ac-opts)
           end-input (js/google.maps.places.Autocomplete. end-elem ac-opts)]

       ;Submit
       (dommy/listen! submit-elem "click" 
                      (fn [] (get-route (.-value start-elem)
                                        (.-value end-elem))))))))
