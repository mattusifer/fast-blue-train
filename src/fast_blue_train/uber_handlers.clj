(ns fast-blue-train.uber-handlers
  (:require [clj-http.client :as client]))

(def uber-token "lLwZN5ojjEB3ISysdObEawgUj6zaLtGhqvM7qKGJ")

(defn uber-price-handler 
  [{{{start-lat "0" start-long "1"} :start
     {end-lat   "0" end-long   "1"} :end} :params}]
  (:body (client/get "https://api.uber.com/v1/estimates/price" 
                     {:accept :json
                      :query-params {:start_latitude start-lat
                                     :start_longitude start-long
                                     :end_latitude end-lat
                                     :end_longitude end-long}
                      :headers {:Authorization (str "Token " uber-token)}})))

(defn uber-time-handler 
  [{{{start-lat "0" start-long "1"} :start
     product_id :product_id} :params}]

  (if (nil? product_id)
    (:body (client/get "https://api.uber.com/v1/estimates/price" 
                       {:accept :json
                        :query-params {:start_latitude start-lat
                                       :start_longitude start-long}
                        :headers {:Authorization (str "Token " uber-token)}}))
    (:body (client/get "https://api.uber.com/v1/estimates/price" 
                       {:accept :json
                        :query-params {:start_latitude start-lat
                                       :start_longitude start-long
                                       :product_id product_id}
                        :headers {:Authorization (str "Token " uber-token)}}))))

