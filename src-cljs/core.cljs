(ns fast-blue-train.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [dommy.core :as dommy :refer-macros [sel1 sel]]))

(declare *map*) 
(declare start-input)
(declare end-input)
(declare places) 

(def directions-service (google.maps.DirectionsService.))

(defn- html-aggregator 
  [old next-step]
  (str old
       "<tr><td>" (.-instructions next-step) "</td>"
       "<td>" (.-text (.-distance next-step)) "</td>"
       "<td>" (.-text (.-duration next-step)) "</tr>"))

(defn- on-place-changed []
  (let [place (.getPlace start-input)]
    (when (.-geometry place)
      (.panTo *map* (.-location (.-geometry place)))
      (.setZoom *map* 15))))

(defn- show-steps [response]
  (let [route (first (.-legs (first (.-routes response))))
        steps (.-steps route)]
    (loop [cur-step (first steps)
           other-steps (rest steps)
           html "<tr><th>Instructions</th><th>Distance</th><th>Duration</th></tr>"]
      (if (empty? other-steps)
        (set! (.-innerHTML (sel1 :#instructions)) html)
        (let [new-html (html-aggregator html cur-step)]
          (recur (first other-steps) (rest other-steps) new-html))))))

(defn- mode-translator [mode]
  (cond 
    (= mode "driving") google.maps.TravelMode.DRIVING
    (= mode "walking") google.maps.TravelMode.WALKING
    (= mode "transit") google.maps.TravelMode.TRANSIT))

(defn- dir-service-handler [res stat]
  (if (= stat google.maps.DirectionsStatus.OK)
    (show-steps res)
    (js/alert (str "Failed due to " stat))))

(defn- get-directions [] 
  (let [mode (.-value (first (filter #(and (= (.-name %) "modes") 
                                           (.-checked %)) 
                                     (sel :input))))
        req (clj->js {"origin" (.-value (sel1 :#startLocationInput))
                      "destination" (.-value (sel1 :#endLocationInput))
                      "travelMode" (mode-translator mode)})]
    (doseq [o (filter #(= (.-name %) "modes") (sel :input))])
    (.route directions-service req dir-service-handler)))

(defn init
  "Initialize Google Map elements"
  []
  ; Map
  (let [map-opts (clj->js {"zoom" 8
                          "center" (google.maps.LatLng. -34.397 150.644)
                          "mapTypeId" "roadmap"})
        ac-opts (clj->js {"types" ["address"]
                          "componentRestrictions" {"country" "us"}})
        map-elem (sel1 :#test-map)
        start-elem (sel1 :#startLocationInput)
        end-elem (sel1 :#endLocationInput)
        submit-elem (sel1 :#directionSubmit)]
    (set! *map* (google.maps.Map. map-elem map-opts))

    ;Start Input
    (set! start-input (google.maps.places.Autocomplete. start-elem ac-opts))
    (set! places (google.maps.places.PlacesService. *map*))
    (.addListener start-input "place_changed" on-place-changed)

    ;End Input
    (set! end-input (google.maps.places.Autocomplete. end-elem ac-opts))
    (set! places (google.maps.places.PlacesService. *map*))
    
    ;Submit
    (dommy/listen! submit-elem "click" get-directions)))

(init)
