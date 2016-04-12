(ns ^{:doc "AngularJS Directive for Producing a Loading Animation"}
  fast-blue-train.directives.loading-overlay-directive
  (:require [cljs.core.async :refer [<!]]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.loadingOverlay [RequestService]
  (obj
   :restrict "E"
   :templateUrl "angular/src/partials/loadingOverlay.html"
   :scope {}
   :controllerAs "vm"
   :bindToController true
   :controller
   (fn []
     (def vm this)
     (! vm.loaderVisible false)

     (! vm.showLoader
        (fn []
          (! vm.loaderVisible true)))
     (! vm.hideLoader
        (fn [] 
          (! vm.loaderVisible false))))
   :link
   (fn [scope elem attr controller]
     ((? RequestService.registerAsSendingRequestObserver) 
      (? controller.showLoader))
     ((? RequestService.registerAsCompletedRequestObserver) 
      (? controller.hideLoader)))))
