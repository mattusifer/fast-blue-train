(ns ^{:doc "AngularJS Service to wrap the Google Maps API"}
  fast-blue-train.services.google-maps-service
  (:use-macros [purnam.core :only [! obj]]
               [gyr.core :only [def.service]]))

(def.service fbm.app.GoogleMapsService [$q]
  (obj
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
