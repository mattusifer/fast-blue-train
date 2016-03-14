(ns ^{:doc "AngularJS Directive for Rendering a Map"}
  fast-blue-train.directives.map-directive
  (:require [fast-blue-train.services.google-maps-service :as maps]
            [cljs.core.async :refer [<!]]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj !]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.map [$q
                            GoogleMapsService]
  (obj
   :restrict "E"
   :templateUrl "angular/src/partials/map.html"
   :scope {}
   :controllerAs "vm"
   :bindToController true
   :controller
   (fn []
     (def vm this))
   :link
   (fn [scope elem attr]
     (let [map-elem (sel1 :#test-map)
           panel-elem (sel1 :#instructions-container)]
       ((.-configureMap GoogleMapsService) map-elem)
       ((.-configureRenderer GoogleMapsService) panel-elem)))))
