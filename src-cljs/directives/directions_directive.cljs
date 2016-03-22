(ns ^{:doc "AngularJS Directive for Getting Directions"}
  fast-blue-train.directives.directions-directive
  (:require [fast-blue-train.services.google-maps-service :as maps]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
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
       (apply min-key (? GoogleMapsService.getDurationFromResponse) 
              routes))

     (defn get-route
       "Get shortest route between start and destination"
       [start end]
       (let [promises (map (partial (? GoogleMapsService.getDirections) 
                                    start end) (available-modes))
             handler (fn [values]
                       ((? GoogleMapsService.displayRoutes) 
                        values ;(get-optimal-route values)
                        )
                       (reset-focus))]
         (.then (.all $q (into-array promises)) handler)))
     vm)

   :link
   (fn [scope elm attr]
     (let [start-elem (sel1 :#startLocationInput)
           end-elem (sel1 :#endLocationInput)
           submit-elem (sel1 :#direction-submit)
           ac-opts (? GoogleMapsService.autocompleteOpts)
           start-input (js/google.maps.places.Autocomplete. start-elem ac-opts)
           end-input (js/google.maps.places.Autocomplete. end-elem ac-opts)]

       ;Submit
       (dommy/listen! submit-elem "click" 
                      (fn [] (get-route (.-value start-elem)
                                        (.-value end-elem))))))))
