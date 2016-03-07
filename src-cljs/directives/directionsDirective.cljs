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
   :controllerAs "view"
   :bindToController true
   :controller 
   (fn [] 
     (declare *map*) 
     (declare start-input)
     (declare end-input)
     (declare places) 

     (def directions-service (js/google.maps.DirectionsService.))
     
     (defn- html-aggregator 
       "create HTML table from directions"
       [old next-step]
       (str old
            "<tr><td>" (.-instructions next-step) "</td>"
            "<td>" (.-text (.-distance next-step)) "</td>"
            "<td>" (.-text (.-duration next-step)) "</tr>"))
     
     (defn- on-place-changed 
       "re-position map based on the value of the start element"
       []
       (let [place (.getPlace start-input)]
         (when (.-geometry place)
           (.panTo *map* (.-location (.-geometry place)))
           (.setZoom *map* 15))))
     
     (defn- show-steps
       "create HTML table from directions"
       [response]
       (let [steps (.-steps response)]
         (loop [cur-step (first steps)
                other-steps (rest steps)
                html "<tr><th>Instructions</th><th>Distance</th><th>Duration</th></tr>"]
           (if (empty? other-steps)
             (set! (.-innerHTML (sel1 :#instructions)) html)
             (let [new-html (html-aggregator html cur-step)]
               (recur (first other-steps) (rest other-steps) new-html))))))

     (defn- get-directions-promise 
       "wrapper function to get a promise from the (callback-driven) Google Maps API"
       [mode]
       (let [deferred (.defer $q)
             dir-service-handler (fn [res stat]
                                   (if (= stat js/google.maps.DirectionsStatus.OK)
                                     (.resolve deferred (first (.-legs (first (.-routes res)))))
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
             get-duration (fn [response] 
                        (.-value (.-duration response)))
             get-min (fn [values] 
                       (show-steps (apply min-key get-duration values)))
             promises (map get-directions-promise modes)]
         (.then (.all $q (into-array promises)) get-min)))
     
     ; main
     (let [map-opts (clj->js {"zoom" 8
                              "center" (js/google.maps.LatLng. -34.397 150.644)
                              "mapTypeId" "roadmap"})
           ac-opts (clj->js {"types" ["address"]
                             "componentRestrictions" {"country" "us"}})
           map-elem (sel1 :#test-map)
           start-elem (sel1 :#startLocationInput)
           end-elem (sel1 :#endLocationInput)
           submit-elem (sel1 :#directionSubmit)]
       (set! *map* (js/google.maps.Map. map-elem map-opts))
       
       ;Start Input
       (set! start-input (js/google.maps.places.Autocomplete. start-elem ac-opts))
       (set! places (js/google.maps.places.PlacesService. *map*))
       (.addListener start-input "place_changed" on-place-changed)
       
       ;End Input
       (set! end-input (js/google.maps.places.Autocomplete. end-elem ac-opts))
       (set! places (js/google.maps.places.PlacesService. *map*))
       
       ;Submit
       (dommy/listen! submit-elem "click" get-route)))))
