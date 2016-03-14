(ns ^{:doc "AngularJS Service to wrap the Google Maps API"}
  fast-blue-train.services.google-maps-service
  (:require [dommy.core :as dommy :refer-macros [sel1]])
  (:use-macros [purnam.core :only [! obj]]
               [gyr.core :only [def.service]]))

(def.service fbm.app.GoogleMapsService [$q]
  (obj 
   :gMap nil
   :renderer nil
   :mapOpts (let [philly (js/google.maps.LatLng. 39.95 -75.1667)]
              (clj->js {"zoom" 10
                        "center" philly
                        "mapTypeId" "roadmap"}))
   :configureMap 
   (fn [map-elem map-opts] 
     (! this.gMap (js/google.maps.Map. map-elem (.-mapOpts this))))
   :configureRenderer (fn [panel-elem]
                        (let [renderer (js/google.maps.DirectionsRenderer.)]
                          (.setMap renderer (.-gMap this))
                          (.setPanel renderer panel-elem)
                          (! this.renderer renderer)))
   :autocompleteOpts (clj->js {"types" ["address"]
                               "componentRestrictions" {"country" "us"}})
   :displayRoute (fn [route]
                   (.setDirections (.-renderer this) route))
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
