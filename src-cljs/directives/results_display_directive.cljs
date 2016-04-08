(ns ^{:doc "AngularJS Directive for Displaying Results"}
  fast-blue-train.directives.results-display-directive
  (:require [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.resultsDisplay [RequestService]
  (obj
   :restrict "E"
   :templateUrl "angular/src/partials/resultsDisplay.html"
   :scope {}
   :controllerAs "vm"
   :bindToController true
   :controller 
   (fn [] 
     (def vm this)
     (! vm.results (clj->js {}))

     (defn setResults [organized-responses]
       (let [just-routes (for [option organized-responses
                               route (second option)]
                           (? (map #(.-request %) (second route))))]
         (! vm.results (clj->js just-routes))))

     ((? RequestService.registerObserver) setResults)
     
     vm)

   :link
   (fn [scope elm attr controller] )))
