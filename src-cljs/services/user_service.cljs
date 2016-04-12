(ns ^{:doc "AngularJS Service for Defining User Preferences"}
  fast-blue-train.services.user-service
  (:require [dommy.core :as dommy :refer-macros [sel1 sel]])
  (:use-macros [purnam.core :only [obj ! ?]]
               [gyr.core :only [def.factory]]))

(def.factory fbm.app.UserService []
  (def user 
    (obj
     :preferences {:age            nil
                   :weight         nil
                   :startLocation  nil
                   :endLocation    nil
                   :carLocation    nil
                   :carMPG         nil
                   :bikeLocation   nil
                   :budget         nil
                   :transPass      nil}
     :setPreference (fn [preference value] 
                      (! user.preferences.|preference| value))))
  user)
