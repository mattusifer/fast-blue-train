(ns ^{:doc "AngularJS Service to wrap the Google Maps API"}
  fast-blue-train.services.google-maps-service
  (:require [dommy.core :as dommy :refer-macros [sel1]])
  (:use-macros [purnam.core :only [! obj ?]]
               [gyr.core :only [def.factory]]))

(def.factory fbm.app.GoogleMapsService [$q]
  (def mapObj
    (obj 
     :gMap nil
     :renderers (clj->js [])
     :polyLines (obj :WALKING   (js/google.maps.Polyline. 
                                 (obj :strokeColor "#C44D58"))
                     :DRIVING   (js/google.maps.Polyline. 
                                 (obj :strokeColor "#FF6B6B"))
                     :TRANSIT   (js/google.maps.Polyline. 
                                 (obj :strokeColor "#556270"))
                     :BICYCLING (js/google.maps.Polyline. 
                                 (obj :strokeColor "#4ECDC4"))
                     :UBER      (js/google.maps.Polyline.
                                 (obj :strokeColor "#C7F464")))
     :mapOpts (let [philly (js/google.maps.LatLng. 39.95 -75.1667)]
                (obj :zoom 12
                     :center philly
                     :mapTypeId "roadmap"))
     :getDurationFromResponse 
     (fn [response]
       (.-value (.-duration (first (.-legs (first (.-routes response)))))))
     :getMilesFromResponse
     (fn [response]
       (let [parse-miles 
             (fn [text]
               (js/parseFloat 
                (.substring text 0 (- (.indexOf text "mi") 1))))]
         (parse-miles 
          (.-text (.-distance (first (.-legs (first (.-routes response)))))))))
     :getTransportModeFromResponse
     (fn [response]
       (? response.request.travelMode))
     :configureMap 
     (fn [map-elem map-opts] 
       (! mapObj.gMap (js/google.maps.Map. map-elem (? mapObj.mapOpts))))
     :autocompleteOpts (obj :types ["address"]
                            :componentRestrictions {"country" "us"})
     :displayRoutes 
     (fn [routes] 
       ; clear map & panel
       (doseq [renderer (? mapObj.renderers)]
         (when (not (nil? renderer))
           (.setMap renderer nil)))
       (! mapObj.renderers (clj->js []))

       ; display new routes
       (doseq [route routes]
         (let [raw-mode ((? mapObj.getTransportModeFromResponse) route)
               mode (if (re-matches #"UBER.*" raw-mode) "UBER" raw-mode)
               renderable-route (if (re-matches #"UBER.*" raw-mode)
                                  (clj->js (assoc-in (js->clj route :keywordize-keys true) 
                                                     [:request :travelMode] "DRIVING"))
                                  route)
               renderer (js/google.maps.DirectionsRenderer. 
                         (obj :polylineOptions 
                              (aget (? mapObj.polyLines) mode)
                             :preserveViewport true))]
           (.setMap renderer (? mapObj.gMap))
           (.setDirections renderer renderable-route)
           (.push (? mapObj.renderers) renderer))))
     :getDirections 
     (fn [start end mode]
       (let [deferred (.defer $q)
             dir-service (js/google.maps.DirectionsService.)
             dir-service-handler 
             (fn [res stat]
               (if (= stat js/google.maps.DirectionsStatus.OK)
                 (.resolve deferred res)
                 (.reject deferred (str "Failed due to " stat))))
             req (clj->js {"origin" start
                           "destination" end
                           "travelMode" mode})]
         (.route dir-service req dir-service-handler)
         (.then (.-promise deferred)
                (fn [resp] resp)
                (fn [err] (.reject $q err)))))))
  mapObj)
