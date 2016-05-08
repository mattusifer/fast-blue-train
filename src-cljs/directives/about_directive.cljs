(ns ^{:doc "AngularJS Directive for Displaying Documentation"}
  fast-blue-train.directives.about-directive
  (:require [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.about []
  (obj
   :restrict "E"
   :templateUrl "angular/src/partials/about.html"
   :scope {}
   :controllerAs "vm"
   :bindToController true
   :controller 
   (fn []
     (def vm this)
     (! vm.popoverVisible false )

     vm)))
