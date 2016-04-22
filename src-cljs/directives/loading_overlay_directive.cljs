(ns ^{:doc "AngularJS Directive for Producing a Loading Animation"}
  fast-blue-train.directives.loading-overlay-directive
  (:require [cljs.core.async :refer [<!]]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.loadingOverlay []
  (obj
   :restrict "E"
   :templateUrl "angular/src/partials/loadingOverlay.html"
   :scope {}
   :controllerAs "vm"
   :bindToController true
   :controller
   (fn [$scope RequestService]
     (def vm this)
     (! vm.loaderVisible false)

     (! vm.showLoader
        (fn []
          (! vm.loaderVisible true)))
     (! vm.hideLoader
        (fn [] 
          (! vm.loaderVisible false)))

     (.$on $scope "sendingRequests" (? vm.showLoader))
     (.$on $scope "requestsComplete" (? vm.hideLoader))

     vm)))
