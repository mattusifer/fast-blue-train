(ns ^{:doc "AngularJS Directive for Displaying Results"}
  fast-blue-train.directives.results-display-directive
  (:require [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.resultsDisplay []
  (obj
   :restrict "E"
   :templateUrl "angular/src/partials/resultsDisplay.html"
   :scope {}
   :controllerAs "vm"
   :bindToController true
   :controller 
   (fn [$scope RequestService CostService] 
     (def vm this)
     (! vm.results (clj->js {}))

     (! vm.setResults 
        (fn [evt args]
          (let [organized-responses (? args.organizedResponses)
                just-routes 
                (for [option organized-responses]
                  (map #(assoc (js->clj (:request %)) 
                               :cost ((? CostService.getMonetaryCost) %)
                               :duration ((? CostService.getDuration) %)) 
                       option))]
            (! vm.results (clj->js just-routes)))))
     
     (.$on $scope "requestsComplete" (? vm.setResults))

     vm)))
