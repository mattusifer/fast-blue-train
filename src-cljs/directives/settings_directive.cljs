(ns ^{:doc "AngularJS Directive for Defining User Preferences"}
  fast-blue-train.directives.settings-directive
  (:require [fast-blue-train.services.user-service :as user]
            [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.controller def.directive]]))

(def.directive fbm.app.settings [UserService]
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
     (! vm.user (? UserService.preferences))

     (! vm.setPreference
        (fn [pref val] ((? UserService.setPreference) pref val))))
   :link
   (fn [scope elem attr])))
