(ns ^{:doc "AngularJS Directive for Getting Directions"}
  fast-blue-train.directives.directions-directive
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj !]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.directions [$q UserService GoogleMapsService]
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

     (defn display-route [route renderer]
       (! vm.initview false)
       (.setDirections renderer route)
       (reset-focus))

     (defn get-route
       "Get shortest route between start and destination"
       [start end renderer]
       (let [promises (map (partial (.-getDirections GoogleMapsService) 
                                    start end) modes)
             handler (fn [values]
                       (display-route (get-optimal-route values) renderer))]
         (.then (.all $q (into-array promises)) handler)))
     
     vm)

   :link
   (fn [scope elm attr]
     (let [philly (js/google.maps.LatLng. 39.95 -75.1667)
           map-opts (clj->js {"zoom" 10
                              "center" philly
                              "mapTypeId" "roadmap"})
           ac-opts (clj->js {"types" ["address"]
                             "componentRestrictions" {"country" "us"}})
           map-elem (sel1 :#test-map)
           start-elem (sel1 :#startLocationInput)
           end-elem (sel1 :#endLocationInput)
           submit-elem (sel1 :#direction-submit)]

       (declare *map*) 
       (declare renderer)
       (declare start-input)
       (declare end-input)
       (declare places) 

       ; configure map and renderer
       (set! renderer (js/google.maps.DirectionsRenderer.))
       (set! *map* (js/google.maps.Map. map-elem map-opts))
       (.setMap renderer *map*)
       (.setPanel renderer (sel1 :#instructions-container))

       ;Start Input
       (set! start-input (js/google.maps.places.Autocomplete. start-elem ac-opts))
       
       ;End Input
       (set! end-input (js/google.maps.places.Autocomplete. end-elem ac-opts))
       (set! places (js/google.maps.places.PlacesService. *map*))
       
       ;Submit
       (dommy/listen! submit-elem "click" 
                      (fn [] (get-route (.-value start-elem)
                                        (.-value end-elem)
                                        renderer)))))))
