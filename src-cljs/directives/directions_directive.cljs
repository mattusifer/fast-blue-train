(ns ^{:doc "AngularJS Directive for Getting Directions"}
  fast-blue-train.directives.directions-directive
  (:require [fast-blue-train.services.google-maps-service :as maps]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.directions [$q 
                                   UserService 
                                   GoogleMapsService
                                   RequestService]
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
     (! vm.user (? UserService.preferences))
     (! vm.setPreference
        (fn [pref val] ((? UserService.setPreference) pref val)))

     (defn get-optimal-route 
       "return the best route"
       [routes]
       (.log js/console "routes")
       (.log js/console (clj->js routes))

       (apply min-key
              #(reduce + 
                       (map (? GoogleMapsService.getDurationFromResponse)
                            %)) routes))
     vm)

   :link
   (fn [scope elm attr controller]
     (let [start-elem (sel1 :#startLocationInput)
           end-elem (sel1 :#endLocationInput)
           submit-elem (sel1 :#direction-submit)
           ac-opts (? GoogleMapsService.autocompleteOpts)
           start-ac 
           (js/google.maps.places.Autocomplete. start-elem ac-opts)
           end-ac
           (js/google.maps.places.Autocomplete. end-elem ac-opts)]

       (.addListener js/google.maps.event start-ac "place_changed"
                     (fn [] (let [place (.getPlace this)]
                              ((? controller.setPreference) 
                               "startLocation"
                               (? place.formatted_address)))))

       (.addListener js/google.maps.event end-ac "place_changed"
                     (fn [] (let [place (.getPlace this)]
                              ((? controller.setPreference) 
                               "endLocation"
                               (? place.formatted_address)))))

       ;Submit 
       (dommy/listen! submit-elem "click" (? RequestService.start))))))
