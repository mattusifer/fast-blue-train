(ns ^{:doc "AngularJS Directive for Defining User Preferences"}
  fast-blue-train.directives.settings-directive
  (:require [fast-blue-train.services.user-service :as user]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.settings [UserService
                                 GoogleMapsService]
  (obj
   :restrict "E"
   :templateUrl "angular/src/partials/settings.html"
   :scope {}
   :controllerAs "vm"
   :bindToController true
   :controller 
   (fn []
     (def vm this)
     (! vm.popoverVisible false )
     
     ;; window into current value of into user preferences
     (! vm.user (? UserService.preferences))

     ;; function to set user preference
     (! vm.setPreference
        (fn [pref val] ((? UserService.setPreference) pref val)))
     vm)
   :link
   (fn [scope elem attr controller]
     (let [ac-opts (? GoogleMapsService.autocompleteOpts)
           car-location-elem (sel1 :#carLocationInput)
           bike-location-elem (sel1 :#bikeLocationInput)
           car-ac (js/google.maps.places.Autocomplete. 
                   car-location-elem ac-opts)
           bike-ac (js/google.maps.places.Autocomplete. 
                    bike-location-elem ac-opts)]

       (.addListener js/google.maps.event car-ac "place_changed"
                     (fn [] (let [place (.getPlace this)]
                              ((? controller.setPreference) 
                               "carLocation"
                               (? place.formatted_address)))))
       (.addListener js/google.maps.event bike-ac "place_changed"
                     (fn [] (let [place (.getPlace this)]
                              ((? controller.setPreference) 
                               "bikeLocation"
                               (? place.formatted_address)))))))))
