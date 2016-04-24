(ns ^{:doc "AngularJS Directive for Displaying Results"}
  fast-blue-train.directives.results-display-directive
  (:require [dommy.core :as dommy :refer-macros [sel1 sel]]
            [goog.string :as gstr]
            [goog.string.format])
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
   (fn [$scope $sce RequestService CostService GoogleMapsService] 
     (def vm this)
     (! vm.results nil)
     (! vm.rawResponses nil)
     (! vm.activeResult nil)
     (! vm.activeResultIndex 0)

     (defn beautify-time [seconds]
       (let [minutes (/ seconds 60)
             hours (/ seconds 3600)]
         (if (>= hours 1) 
           (str (Math/floor hours) 
                (if (= (Math/floor hours) 1) 
                  " hour"
                  " hours")
                (if (> (- (Math/floor minutes) 
                          (* (Math/floor hours) 60)) 0)
                  (str " "
                       (Math/floor
                        (- minutes 
                           (* (Math/floor hours) 60)))
                       " mins")))
           (str (Math/floor minutes) " mins"))))

     (defn beautify-money [raw-dollars]
       (gstr/format "%10.2f" raw-dollars))

     (! vm.makeRouteReadable
        (fn [route]
          (.trustAsHtml
           $sce
           (str
            (condp re-matches (? route.travelMode)
              #"WALKING" "Walk"
              #"DRIVING" "Drive"
              #"BICYCLING" "Bike"
              #"UBER.*" (str "Take an "
                             (last 
                              (clojure.string/split (? route.travelMode) #" - ")))
              #"TRANSIT" "Take public transit")
            " from <span class='bold'>"
            (first (clojure.string/split (? route.origin) #","))
            "</span> to <span class='bold'>"
            (first (clojure.string/split (? route.destination) #","))
            "</span><ul><li>Cost: $"
            (beautify-money (? route.cost))
            "</li><li>Duration: "
            (beautify-time (? route.duration))
            "</li></ul>"))))

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
                         :totalCost 
                         (beautify-money (reduce + (map (? CostService.getMonetaryCost) option)))
                         :totalDuration 
                         (beautify-time 
                          (reduce + (map #((? CostService.getDuration) %)  option)))))]
            (! vm.rawResponses (clj->js organized-responses))
            (! vm.results (clj->js just-routes))
            (! vm.activeResult (aget (? vm.results) 0)))))
     
     (.$on $scope "requestsComplete" (? vm.setResults))

     vm)
   :link
   (fn [scope elm attr controller]
     )))
