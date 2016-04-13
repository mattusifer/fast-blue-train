(ns fast-blue-train.handler
  (:require [fast-blue-train.uber-handlers :as uber]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [ring.middleware.json :as ring-json]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn wrap-root-index [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (= "/" %) "/index.html" %)))))

(defroutes app-routes
  (GET "/uber-price" [] uber/uber-price-handler)
  (GET "/uber-time" [] uber/uber-time-handler)
  (route/not-found "Page not found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      (wrap-root-index)))
