(ns ^{:doc "AngularJS Directive for Getting Directions"}
  fast-blue-train.core
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [f.n obj !]]
               [gyr.core :only [def.controller def.directive
                                def.module]]))

(def.module fbm.app [])

(def.directive fbm.app.directions [$q]
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

     (declare *map*) 
     (declare renderer)
     (declare start-input)
     (declare end-input)
     (declare places) 

     (def directions-service (js/google.maps.DirectionsService.))
     
     (defn- refresh-map []
       ;; TODO -- figure out how to refresh the map
       ;; (let [center (.getCenter *map*)]
       ;;   (.trigger js/google.maps.event *map* "resize")
       ;;   (.panTo *map* center))
       (.focus (sel1 :#startLocationInput)))
     
     (defn- get-directions-promise 
       "wrapper function to get a promise from the (callback-driven) Google Maps API"
       [mode]
       (let [deferred (.defer $q)
             dir-service-handler (fn [res stat]
                                   (if (= stat js/google.maps.DirectionsStatus.OK)
                                     (.resolve deferred res)
                                     (.reject deferred (str "Failed due to " stat))))
             req (clj->js {"origin" (.-value (sel1 :#startLocationInput))
                           "destination" (.-value (sel1 :#endLocationInput))
                           "travelMode" mode})]
         (.route directions-service req dir-service-handler)
         (.then (.-promise deferred)
                (fn [resp] resp)
                (fn [err] (.reject $q err)))))

     (defn- get-route
       "Get shortest route between start and destination"
       []
       (let [modes [js/google.maps.TravelMode.DRIVING
                    js/google.maps.TravelMode.WALKING
                    js/google.maps.TravelMode.TRANSIT]
             promises (map get-directions-promise modes)
             get-duration (fn [res] 
                        (.-value 
                         (.-duration 
                          (first (.-legs (first (.-routes res)))))))
             disp-route (fn [values] 
                          (let [min (apply min-key get-duration values)]
                            (! vm.initview false)

                            (.setDirections renderer min)
                            (refresh-map)))]
         (.then (.all $q (into-array promises)) disp-route)))
     
     ; main
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

       ; configure map and renderer
       (set! renderer (js/google.maps.DirectionsRenderer.))
       (set! *map* (js/google.maps.Map. map-elem map-opts))
       (.setMap renderer *map*)
       (.setPanel renderer (sel1 :#instructions-container))

       ;Start Input
       (set! start-input (js/google.maps.places.Autocomplete. start-elem ac-opts))
       (set! places (js/google.maps.places.PlacesService. *map*))
       
       ;End Input
       (set! end-input (js/google.maps.places.Autocomplete. end-elem ac-opts))
       (set! places (js/google.maps.places.PlacesService. *map*))
       
       ;Submit
       (dommy/listen! submit-elem "click" 
                      get-route)

       vm))
   :link
   (fn [scope elm attr])))
