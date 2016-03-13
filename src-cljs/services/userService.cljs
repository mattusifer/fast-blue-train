(ns ^{:doc "AngularJS Service for Defining User Preferences"}
  fast-blue-train.services.user-service
  (:use-macros [purnam.core :only [obj !]]
               [gyr.core :only [def.service]]))

(def.service fbm.app.UserService []
  (obj
   :preferences {:hasCar true
                 :carLocation nil
                 :carMPG 23
                 :hasBike true
                 :bikeLocation nil
                 :budget nil}
   :setPreference (fn [preference value]
                    (assoc-in this [:preferences (keyword preference)]
                              value))))

