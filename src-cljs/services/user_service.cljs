(ns ^{:doc "AngularJS Service for Defining User Preferences"}
  fast-blue-train.services.user-service
  (:require [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.factory]]))

(def.factory fbm.app.UserService []
  (def user 
    (obj
     :preferences {:startLocation nil
                   :endLocation nil
                   :hasCar false
                   :carLocation nil
                   :carMPG nil
                   :hasBike false
                   :bikeLocation nil
                   :budget nil}
     :setPreference (fn [preference value] 
                      (! user.preferences.|preference| value))))
  user)
