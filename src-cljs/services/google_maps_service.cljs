(ns ^{:doc "AngularJS Service to wrap the Google Maps API"}
  fast-blue-train.services.google-maps-service
  (:require [dommy.core :as dommy :refer-macros [sel1]])
  (:use-macros [purnam.core :only [! obj ?]]
               [gyr.core :only [def.factory]]))

(def.factory fbm.app.GoogleMapsService [$q]
  (def mapObj
    (obj 
     :gMap nil
     :panel nil
     :renderers (clj->js [])
     :polyLines (obj :WALKING (js/google.maps.Polyline. 
                               (obj :strokeColor "#FF0000"))
                     :DRIVING (js/google.maps.Polyline. 
                               (obj :strokeColor "#00FF00"))
                     :TRANSIT (js/google.maps.Polyline. 
                               (obj :strokeColor "#0000FF"))
                     :BICYCLING (js/google.maps.Polyline. 
                                 (obj :strokeColor "#000000")))
     :mapOpts (let [philly (js/google.maps.LatLng. 39.95 -75.1667)]
                (obj :zoom 10
                     :center philly
                     :mapTypeId "roadmap"))
     :getDurationFromResponse 
     (fn [response]
       (.-value (.-duration (first (.-legs (first (.-routes response)))))))
     :getTransportModeFromResponse
     (fn [response]
       (? response.request.travelMode))
     :configureMap 
     (fn [map-elem map-opts] 
       (! mapObj.gMap (js/google.maps.Map. map-elem (? mapObj.mapOpts))))
     :configurePanel
     (fn [panel-elem] (! mapObj.panel panel-elem))
     :autocompleteOpts (obj :types ["address"]
                            :componentRestrictions {"country" "us"})
     :displayRoutes 
     (fn [routes] 
       ; clear map & panel
       (doseq [renderer (? mapObj.renderers)]
         (when (not (nil? renderer))
           (.setMap renderer nil)) (.setPanel renderer nil))
       (! mapObj.renderers (clj->js []))

       ; display new routes
       (doseq [route routes]
         (let [mode ((? mapObj.getTransportModeFromResponse) route)
               renderer (js/google.maps.DirectionsRenderer. 
                         (obj :polylineOptions 
                              (aget (? mapObj.polyLines) mode)))]
           (.setMap renderer (? mapObj.gMap))
           (.setPanel renderer (? mapObj.panel))
           (.setDirections renderer route)
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
