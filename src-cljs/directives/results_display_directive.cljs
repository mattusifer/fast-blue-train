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
   (fn [$scope RequestService CostService GoogleMapsService] 
     (def vm this)
     (! vm.results nil)
     (! vm.rawResponses nil)
     (! vm.activeResult nil)
     (! vm.activeResultIndex 0)

     (! vm.setActiveResult 
        (fn [result]
          (let [index (.indexOf (? vm.results) result)]
            ((? GoogleMapsService.displayRoutes) (aget (? vm.rawResponses) index))
            (! vm.activeResult result)
            (! vm.activeResultIndex index))))

     (! vm.setResults 
        (fn [evt args]
          (let [organized-responses (? args.organizedResponses)
                just-routes 
                (for [option organized-responses]
                  (assoc {}
                         :routes
                         (map #(assoc (js->clj (:request %)) 
                                      :cost ((? CostService.getMonetaryCost) %)
                                      :duration ((? CostService.getDuration) %)) 
                              option)
                         :totalCost (reduce + (map (? CostService.getMonetaryCost) option))))]
            (! vm.rawResponses (clj->js organized-responses))
            (! vm.results (clj->js just-routes))
            (! vm.activeResult (aget (? vm.results) 0)))))
     
     (.$on $scope "requestsComplete" (? vm.setResults))

     vm)
   :link
   (fn [scope elm attr controller]
     )))
